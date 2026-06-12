import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionService } from '../../auth/session.service';
import {
  CycleProgressRequest,
  DayOfWeek,
  LiftType,
  PrescribedWorkout,
  WorkoutCycleApiService,
  WorkoutCycleRequest,
  WorkoutCycleResponse,
} from '../../api/workout-cycle-api.service';
import { ActivatedRoute, Router } from '@angular/router';
import { OnboardingFlowService } from '../../auth/onboarding-flow.service';

const LIFTS: LiftType[] = ['BENCH', 'SQUAT', 'DEADLIFT', 'SHOULDER_PRESS'];
const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];


@Component({
  selector: 'app-cycles-page',
  imports: [FormsModule],
  templateUrl: './cycles-page.component.html',
  styleUrl: './cycles-page.component.scss',
})
export class CyclesPageComponent {
  protected readonly activeCycle = signal<WorkoutCycleResponse | null>(null);
  protected readonly history = signal<WorkoutCycleResponse[]>([]);
  protected readonly prescribedWorkout = signal<PrescribedWorkout | null>(null);
  protected readonly loading = signal(false);
  protected readonly progressLoading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly date = signal(new Date().toISOString().slice(0, 10));
  protected readonly onboardingMode = signal(false);
  protected readonly form = signal<WorkoutCycleRequest>({
    startDate: new Date().toISOString().slice(0, 10),
    benchDay: 'MONDAY',
    squatDay: 'WEDNESDAY',
    deadliftDay: 'FRIDAY',
    shoulderPressDay: 'TUESDAY',
  });
  protected readonly liftOutcomes = signal<Record<LiftType, boolean>>({
    BENCH: false,
    SQUAT: false,
    DEADLIFT: false,
    SHOULDER_PRESS: false,
  });
  protected readonly lifts = LIFTS;
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

  protected updateOutcome(lift: LiftType, value: boolean): void {
    this.liftOutcomes.update((current) => ({ ...current, [lift]: value }));
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
      const [activeCycle, history, prescribedWorkout] = await Promise.all([
        this.workoutCycleApi.getActiveCycle(firebaseUid).catch(() => null),
        this.workoutCycleApi.getCycleHistory(firebaseUid).catch(() => []),
        this.workoutCycleApi.getPrescribedWorkout(firebaseUid, this.date()).catch(() => null),
      ]);

      this.activeCycle.set(activeCycle);
      this.history.set(history);
      this.prescribedWorkout.set(prescribedWorkout);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load cycle data.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async refreshPrescribedWorkout(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const prescribedWorkout = await this.workoutCycleApi.getPrescribedWorkout(firebaseUid, this.date());
      this.prescribedWorkout.set(prescribedWorkout);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load prescribed workout.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async progressCycle(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.progressLoading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const request: CycleProgressRequest = {
        liftOutcomes: this.liftOutcomes(),
      };
      const updatedCycle = await this.workoutCycleApi.progressToNextCycle(firebaseUid, request);
      this.activeCycle.set(updatedCycle);
      this.success.set('Cycle progress submitted.');
      await this.load(firebaseUid);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to progress cycle.');
    } finally {
      this.progressLoading.set(false);
    }
  }
}
