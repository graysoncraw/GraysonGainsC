import { Injectable, computed, signal } from '@angular/core';

import { PersonalRecordApiService } from '../api/personal-record-api.service';
import { WorkoutCycleApiService } from '../api/workout-cycle-api.service';
import { UserApiService, UserProfileResponse } from '../api/user-api.service';
import { WorkoutScheduleApiService } from '../api/workout-schedule-api.service';
import { FirebaseAuthService } from './firebase-auth.service';
import { SessionService } from './session.service';

export type OnboardingRoute = '/auth' | '/setup/profile' | '/setup/personal-records' | '/setup/schedule' | '/setup/finish' | '/app';

@Injectable({ providedIn: 'root' })
export class OnboardingFlowService {
  private readonly resolvingSignal = signal(false);
  private readonly lastRouteSignal = signal<OnboardingRoute>('/auth');

  readonly resolving = computed(() => this.resolvingSignal());
  readonly lastRoute = computed(() => this.lastRouteSignal());

  constructor(
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly session: SessionService,
    private readonly userApi: UserApiService,
    private readonly personalRecordApi: PersonalRecordApiService,
    private readonly workoutScheduleApi: WorkoutScheduleApiService,
    private readonly workoutCycleApi: WorkoutCycleApiService,
  ) {}

  async resolveRoute(): Promise<OnboardingRoute> {
    this.resolvingSignal.set(true);

    try {
      await this.firebaseAuth.waitUntilReady();
      await this.firebaseAuth.refreshIdToken();

      const snapshot = this.firebaseAuth.snapshot();
      if (!snapshot) {
        this.session.clearSessionState();
        return this.setRoute('/auth');
      }

      const firebaseUid = snapshot.firebaseUid;
      const backendUser = await this.loadUser(firebaseUid);
      if (!backendUser) {
        this.session.setBackendUser(null);
        return this.setRoute('/setup/profile');
      }

      this.session.setBackendUser(backendUser);

      if (!(await this.exists(() => this.personalRecordApi.getPersonalRecord(firebaseUid)))) {
        return this.setRoute('/setup/personal-records');
      }

      if (!(await this.exists(() => this.workoutScheduleApi.getWorkoutSchedule(firebaseUid)))) {
        return this.setRoute('/setup/schedule');
      }

      if (!(await this.exists(() => this.workoutCycleApi.getActiveCycle(firebaseUid)))) {
        return this.setRoute('/setup/finish');
      }

      return this.setRoute('/app');
    } finally {
      this.resolvingSignal.set(false);
    }
  }

  async getCurrentRoute(): Promise<OnboardingRoute> {
    return this.resolveRoute();
  }

  private async loadUser(firebaseUid: string): Promise<UserProfileResponse | null> {
    try {
      return await this.userApi.getUserByFirebaseUid(firebaseUid);
    } catch (error) {
      if (this.isNotFound(error)) {
        return null;
      }
      throw error;
    }
  }

  private async exists(loader: () => Promise<unknown>): Promise<boolean> {
    try {
      await loader();
      return true;
    } catch (error) {
      if (this.isNotFound(error)) {
        return false;
      }
      throw error;
    }
  }

  private isNotFound(error: unknown): boolean {
    return Boolean(error && typeof error === 'object' && 'status' in error && (error as { status?: number }).status === 404);
  }

  private setRoute(route: OnboardingRoute): OnboardingRoute {
    this.lastRouteSignal.set(route);
    return route;
  }
}
