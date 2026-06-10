import { Injectable, computed, signal } from '@angular/core';
import {
  Auth,
  GoogleAuthProvider,
  User,
  createUserWithEmailAndPassword,
  getAuth,
  getIdToken,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
  Unsubscribe,
} from 'firebase/auth';
import { initializeApp } from 'firebase/app';

import { FIREBASE_WEB_CONFIG } from './app-config';

@Injectable({ providedIn: 'root' })
export class FirebaseAuthService {
  private readonly authSignal = signal<Auth | null>(null);
  private readonly userSignal = signal<User | null>(null);
  private readonly idTokenSignal = signal('');
  private readonly errorSignal = signal('');
  private readonly readySignal = signal(false);
  private readonly readyPromise: Promise<void>;
  private readyResolver: (() => void) | null = null;
  private authStateUnsubscribe: Unsubscribe | null = null;

  readonly user = computed(() => this.userSignal());
  readonly error = computed(() => this.errorSignal());
  readonly ready = computed(() => this.readySignal());
  readonly firebaseUid = computed(() => this.userSignal()?.uid ?? '');
  readonly idToken = computed(() => this.idTokenSignal());

  constructor() {
    this.readyPromise = new Promise<void>((resolve) => {
      this.readyResolver = resolve;
    });

    this.bootstrap();
  }

  async signUpWithEmail(email: string, password: string, displayName?: string): Promise<void> {
    const auth = this.ensureAuth();
    this.clearError();

    try {
      const credential = await createUserWithEmailAndPassword(auth, email, password);
      if (displayName && displayName.trim()) {
        await updateProfile(credential.user, { displayName: displayName.trim() });
      }

      await this.syncUserState(credential.user, true);
    } catch (error) {
      this.setError();
      throw error;
    }
  }

  async signInWithEmail(email: string, password: string): Promise<void> {
    const auth = this.ensureAuth();
    this.clearError();

    try {
      const credential = await signInWithEmailAndPassword(auth, email, password);
      await this.syncUserState(credential.user, true);
    } catch (error) {
      this.setError();
      throw error;
    }
  }

  async signInWithGoogle(): Promise<void> {
    const auth = this.ensureAuth();
    this.clearError();

    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      const credential = await signInWithPopup(auth, provider);
      await this.syncUserState(credential.user, true);
    } catch (error) {
      this.setError();
      throw error;
    }
  }

  async signOut(): Promise<void> {
    const auth = this.ensureAuth();
    this.clearError();

    try {
      await signOut(auth);
      this.clearUserState();
    } catch (error) {
      this.setError();
      throw error;
    }
  }

  async refreshIdToken(): Promise<void> {
    const user = this.userSignal();
    if (!user) {
      this.idTokenSignal.set('');
      return;
    }

    await this.syncUserState(user, true);
  }

  async waitUntilReady(): Promise<void> {
    if (this.readySignal()) {
      return;
    }

    await this.readyPromise;
  }

  private bootstrap(): void {
    const config = FIREBASE_WEB_CONFIG;
    const auth = getAuth(initializeApp(config));
    this.authSignal.set(auth);

    if (this.authStateUnsubscribe) {
      this.authStateUnsubscribe();
    }

    this.authStateUnsubscribe = onAuthStateChanged(auth, async (user) => {
      try {
        if (user) {
          await this.syncUserState(user, false);
        } else {
          this.clearUserState();
        }
      } catch (error) {
        this.setError();
      } finally {
        this.markReady();
      }
    });
  }

  private async syncUserState(user: User, forceRefresh: boolean): Promise<void> {
    this.userSignal.set(user);
    this.idTokenSignal.set(await getIdToken(user, forceRefresh));
  }

  private clearUserState(): void {
    this.userSignal.set(null);
    this.idTokenSignal.set('');
  }

  private markReady(): void {
    if (this.readySignal()) {
      return;
    }

    this.readySignal.set(true);
    this.readyResolver?.();
    this.readyResolver = null;
  }

  private ensureAuth(): Auth {
    const auth = this.authSignal();
    if (!auth) {
      throw new Error('Firebase is not ready yet.');
    }

    return auth;
  }

  private clearError(): void {
    this.errorSignal.set('');
  }

  private setError(): void {
    this.errorSignal.set('Authentication failed.');
  }
}
