import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { OnboardingFlowService } from '../../auth/onboarding-flow.service';
import { SessionService } from '../../auth/session.service';
import {
  DayOfWeek,
  LiftType,
  WorkoutCycleApiService,
  WorkoutCycleRequest,
  WorkoutCycleResponse,
} from '../../api/workout-cycle-api.service';

const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

@Component({
  selector: 'app-cycles-page',
  imports: [FormsModule],
  templateUrl: './cycles-page.component.html',
  styleUrl: './cycles-page.component.scss',
})
export class CyclesPageComponent {
  protected readonly activeCycle = signal<WorkoutCycleResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly onboardingMode = signal(false);
  protected readonly form = signal<WorkoutCycleRequest>(this.createDefaultForm());

  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly days = DAYS;

  constructor(
    private readonly session: SessionService,
    private readonly workoutCycleApi: WorkoutCycleApiService,
    private readonly route: ActivatedRoute,
    private readonly onboardingFlow: OnboardingFlowService,
    private readonly router: Router,
  ) {
    this.onboardingMode.set(this.route.snapshot.data['onboarding'] === true);

    effect(() => {
      const firebaseUid = this.firebaseUid();
      if (firebaseUid) {
        void this.load(firebaseUid);
      }
    });
  }

  protected updateField<K extends keyof WorkoutCycleRequest>(field: K, value: WorkoutCycleRequest[K]): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  protected updateNumberField(field: LiftType, value: string): void {
    this.form.update((current) => ({
      ...current,
      [field]: value === '' ? 0 : Number(value),
    }));
  }

  protected async saveSchedule(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const request = this.form();
      const updated = this.activeCycle()
        ? await this.workoutCycleApi.updateActiveWorkoutCycle(firebaseUid, request)
        : await this.workoutCycleApi.createWorkoutCycle(firebaseUid, request);

      this.activeCycle.set(updated);
      this.form.set({
        startDate: updated.startDate,
        benchDay: updated.benchDay,
        squatDay: updated.squatDay,
        deadliftDay: updated.deadliftDay,
        shoulderPressDay: updated.shoulderPressDay,
      });
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

  protected async load(firebaseUid = this.firebaseUid()): Promise<void> {
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const activeCycle = await this.workoutCycleApi.getActiveCycle(firebaseUid).catch(() => null);
      this.activeCycle.set(activeCycle);

      if (activeCycle) {
        this.form.set({
          startDate: activeCycle.startDate,
          benchDay: activeCycle.benchDay,
          squatDay: activeCycle.squatDay,
          deadliftDay: activeCycle.deadliftDay,
          shoulderPressDay: activeCycle.shoulderPressDay,
        });
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load cycle data.');
    } finally {
      this.loading.set(false);
    }
  }

  private createDefaultForm(): WorkoutCycleRequest {
    return {
      startDate: new Date().toISOString().slice(0, 10),
      benchDay: 'MONDAY',
      squatDay: 'WEDNESDAY',
      deadliftDay: 'FRIDAY',
      shoulderPressDay: 'TUESDAY',
    };
  }
}
