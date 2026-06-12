import { Injectable, computed, signal } from '@angular/core';

import { PersonalRecordApiService } from '../api/personal-record-api.service';
import { WorkoutCycleApiService } from '../api/workout-cycle-api.service';
import { UserApiService, UserProfileResponse } from '../api/user-api.service';
import { FirebaseAuthService } from './firebase-auth.service';
import { SessionService } from './session.service';

export type OnboardingRoute = '/auth' | '/setup/profile' | '/setup/personal-records' | '/setup/cycles' | '/setup/finish' | '/app';

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
    private readonly workoutCycleApi: WorkoutCycleApiService,
  ) {}

  async resolveRoute(): Promise<OnboardingRoute> {
    this.resolvingSignal.set(true);

    try {
      await this.firebaseAuth.waitUntilReady();
      await this.firebaseAuth.refreshIdToken();

      const user = this.firebaseAuth.user();
      if (!user) {
        this.session.clearSessionState();
        return this.setRoute('/auth');
      }

      const firebaseUid = user.uid;
      const backendUser = await this.loadUser(firebaseUid);
      if (!backendUser) {
        this.session.setBackendUser(null);
        return this.setRoute('/setup/profile');
      }

      this.session.setBackendUser(backendUser);

      if (!(await this.exists(() => this.personalRecordApi.getPersonalRecord(firebaseUid)))) {
        return this.setRoute('/setup/personal-records');
      }

      if (!(await this.exists(() => this.workoutCycleApi.getActiveCycle(firebaseUid)))) {
        return this.setRoute('/setup/cycles');
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
