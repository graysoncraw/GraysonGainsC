import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SessionService } from '../../auth/session.service';
import { PrescribedWorkout, WorkoutCycleApiService, WorkoutCycleResponse } from '../../api/workout-cycle-api.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent {
  protected readonly activeCycle = signal<WorkoutCycleResponse | null>(null);
  protected readonly prescribedWorkout = signal<PrescribedWorkout | null>(null);
  protected readonly historyCount = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected readonly user = computed(() => this.session.backendUser());

  constructor(
    private readonly session: SessionService,
    private readonly workoutCycleApi: WorkoutCycleApiService,
  ) {
    void this.load();
  }

  protected async load(): Promise<void> {
    const firebaseUid = this.session.firebaseUid();
    if (!firebaseUid) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const today = new Date().toISOString().slice(0, 10);
      const [activeCycle, prescribedWorkout, history] = await Promise.all([
        this.workoutCycleApi.getActiveCycle(firebaseUid).catch(() => null),
        this.workoutCycleApi.getPrescribedWorkout(firebaseUid, today).catch(() => null),
        this.workoutCycleApi.getCycleHistory(firebaseUid).catch(() => []),
      ]);

      this.activeCycle.set(activeCycle);
      this.prescribedWorkout.set(prescribedWorkout);
      this.historyCount.set(history.length);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load dashboard.');
    } finally {
      this.loading.set(false);
    }
  }
}
