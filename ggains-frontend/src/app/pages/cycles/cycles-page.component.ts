import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionService } from '../../auth/session.service';
import {
  CycleProgressRequest,
  LiftType,
  PrescribedWorkout,
  WorkoutCycleApiService,
  WorkoutCycleResponse,
} from '../../api/workout-cycle-api.service';

const LIFTS: LiftType[] = ['BENCH', 'SQUAT', 'DEADLIFT', 'SHOULDER_PRESS'];

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
  protected readonly liftOutcomes = signal<Record<LiftType, boolean>>({
    BENCH: false,
    SQUAT: false,
    DEADLIFT: false,
    SHOULDER_PRESS: false,
  });
  protected readonly lifts = LIFTS;
  protected readonly firebaseUid = computed(() => this.session.firebaseUid());

  constructor(
    private readonly session: SessionService,
    private readonly workoutCycleApi: WorkoutCycleApiService,
  ) {
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
