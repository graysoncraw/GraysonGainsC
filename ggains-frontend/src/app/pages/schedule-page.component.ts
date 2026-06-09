import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { OnboardingFlowService } from '../auth/onboarding-flow.service';
import { SessionService } from '../auth/session.service';
import {
  DayOfWeek,
  WorkoutScheduleApiService,
  WorkoutScheduleRequest,
  WorkoutScheduleResponse,
} from '../api/workout-schedule-api.service';

const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

@Component({
  selector: 'app-schedule-page',
  imports: [FormsModule],
  templateUrl: './schedule-page.component.html',
  styleUrl: './schedule-page.component.scss',
})
export class SchedulePageComponent {
  protected readonly schedule = signal<WorkoutScheduleResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly days = DAYS;
  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly onboardingMode = signal(false);
  protected readonly form = signal<WorkoutScheduleRequest>({
    cycleStartDate: new Date().toISOString().slice(0, 10),
    benchDay: 'MONDAY',
    squatDay: 'WEDNESDAY',
    deadliftDay: 'FRIDAY',
    shoulderPressDay: 'TUESDAY',
  });

  constructor(
    private readonly session: SessionService,
    private readonly workoutScheduleApi: WorkoutScheduleApiService,
    private readonly onboardingFlow: OnboardingFlowService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {
    this.onboardingMode.set(this.route.snapshot.data['onboarding'] === true);
    effect(() => {
      const firebaseUid = this.firebaseUid();
      if (firebaseUid) {
        void this.load(firebaseUid);
      }
    });
  }

  protected updateField<K extends keyof WorkoutScheduleRequest>(field: K, value: WorkoutScheduleRequest[K]): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  protected async load(firebaseUid = this.firebaseUid()): Promise<void> {
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const schedule = await this.workoutScheduleApi.getWorkoutSchedule(firebaseUid);
      this.schedule.set(schedule);
      this.form.set({
        cycleStartDate: schedule.cycleStartDate,
        benchDay: schedule.benchDay,
        squatDay: schedule.squatDay,
        deadliftDay: schedule.deadliftDay,
        shoulderPressDay: schedule.shoulderPressDay,
      });
    } catch (error) {
      this.schedule.set(null);
      if (!this.isNotFound(error)) {
        this.error.set(error instanceof Error ? error.message : 'Unable to load workout schedule.');
      }
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const request = this.form();
      const updated = this.schedule()
        ? await this.workoutScheduleApi.updateWorkoutSchedule(firebaseUid, request)
        : await this.workoutScheduleApi.createWorkoutSchedule(firebaseUid, request);

      this.schedule.set(updated);
      this.success.set('Schedule saved.');

      if (this.onboardingMode()) {
        const nextRoute = await this.onboardingFlow.resolveRoute();
        await this.router.navigateByUrl(nextRoute);
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to save schedule.');
    } finally {
      this.loading.set(false);
    }
  }

  private isNotFound(error: unknown): boolean {
    return Boolean(error && typeof error === 'object' && 'status' in error && (error as { status?: number }).status === 404);
  }
}
