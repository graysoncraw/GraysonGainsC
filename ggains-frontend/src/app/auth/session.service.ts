import { computed, effect, Injectable, signal } from '@angular/core';

import { UserApiService, UserProfileResponse } from '../api/user-api.service';
import { FirebaseAuthService } from './firebase-auth.service';

type SessionStatus = 'idle' | 'loading' | 'ready' | 'error';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly statusSignal = signal<SessionStatus>('idle');
  private readonly backendUserSignal = signal<UserProfileResponse | null>(null);
  private readonly messageSignal = signal('');
  private readonly lastBootstrapKeySignal = signal('');

  readonly status = computed(() => this.statusSignal());
  readonly backendUser = computed(() => this.backendUserSignal());
  readonly message = computed(() => this.messageSignal());
  readonly isReady = computed(() => this.statusSignal() === 'ready');
  readonly firebaseUid = computed(() => this.firebaseAuth.firebaseUid());

  constructor(
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly userApi: UserApiService,
  ) {
    effect(() => {
      if (!this.firebaseAuth.user()) {
        this.backendUserSignal.set(null);
        this.statusSignal.set('idle');
        this.messageSignal.set('');
        this.lastBootstrapKeySignal.set('');
      }
    });
  }

  async bootstrapCurrentSession(): Promise<UserProfileResponse | null> {
    await this.firebaseAuth.waitUntilReady();
    await this.firebaseAuth.refreshIdToken();

    const user = this.firebaseAuth.user();
    if (!user) {
      this.backendUserSignal.set(null);
      this.statusSignal.set('idle');
      this.messageSignal.set('');
      this.lastBootstrapKeySignal.set('');
      return null;
    }

    const bootstrapKey = `${user.uid}:${this.firebaseAuth.idToken()}`;
    if (this.lastBootstrapKeySignal() === bootstrapKey && this.backendUserSignal()) {
      return this.backendUserSignal();
    }

    this.statusSignal.set('loading');
    this.messageSignal.set('');

    try {
      const backendUser = await this.userApi.getUserByFirebaseUid(user.uid);

      this.backendUserSignal.set(backendUser);
      this.statusSignal.set('ready');
      this.lastBootstrapKeySignal.set(bootstrapKey);
      return backendUser;
    } catch (error) {
      this.backendUserSignal.set(null);
      this.statusSignal.set('error');
      this.messageSignal.set(error instanceof Error ? error.message : 'Failed to bootstrap session.');
      this.lastBootstrapKeySignal.set('');
      throw error;
    }
  }

  clearSessionState(): void {
    this.backendUserSignal.set(null);
    this.statusSignal.set('idle');
    this.messageSignal.set('');
    this.lastBootstrapKeySignal.set('');
  }

  setBackendUser(user: UserProfileResponse | null): void {
    this.backendUserSignal.set(user);
    if (user) {
      this.statusSignal.set('ready');
    }
  }
}
