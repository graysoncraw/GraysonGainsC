import { Injectable, computed, signal } from '@angular/core';
import {
  Auth,
  GoogleAuthProvider,
  User,
  createUserWithEmailAndPassword,
  getAuth,
  onAuthStateChanged,
  updateProfile,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  Unsubscribe,
} from 'firebase/auth';
import { FirebaseApp, FirebaseOptions, getApp, getApps, initializeApp } from 'firebase/app';

import { FIREBASE_WEB_CONFIG } from './app-config';
import { AuthSnapshot, FirebaseWebConfig } from './firebase-auth.types';

const AUTH_SNAPSHOT_STORAGE_KEY = 'graysongains.authSnapshot';

@Injectable({ providedIn: 'root' })
export class FirebaseAuthService {
  private readonly authSignal = signal<Auth | null>(null);
  private readonly userSignal = signal<User | null>(null);
  private readonly snapshotSignal = signal<AuthSnapshot | null>(this.loadStoredSnapshot());
  private readonly errorSignal = signal<string>('');
  private readonly readySignal = signal(false);
  private authStateUnsubscribe: Unsubscribe | null = null;

  readonly user = computed(() => this.userSignal());
  readonly snapshot = computed(() => this.snapshotSignal());
  readonly error = computed(() => this.errorSignal());
  readonly ready = computed(() => this.readySignal());
  readonly firebaseUid = computed(() => this.snapshotSignal()?.firebaseUid ?? '');
  readonly idToken = computed(() => this.snapshotSignal()?.idToken ?? '');

  constructor() {
    this.bootstrap();
  }

  async signUpWithEmail(email: string, password: string, displayName?: string): Promise<void> {
    const auth = this.ensureAuth();
    this.setError('');
    const credential = await createUserWithEmailAndPassword(auth, email, password);
    if (displayName && displayName.trim()) {
      await updateProfile(credential.user, { displayName: displayName.trim() });
    }
    await this.syncSnapshot(credential.user);
  }

  async signInWithEmail(email: string, password: string): Promise<void> {
    const auth = this.ensureAuth();
    this.setError('');
    const credential = await signInWithEmailAndPassword(auth, email, password);
    await this.syncSnapshot(credential.user);
  }

  async signInWithGoogle(): Promise<void> {
    const auth = this.ensureAuth();
    this.setError('');
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({ prompt: 'select_account' });
    const credential = await signInWithPopup(auth, provider);
    await this.syncSnapshot(credential.user);
  }

  async signOut(): Promise<void> {
    const auth = this.ensureAuth();
    await signOut(auth);
    this.userSignal.set(null);
    this.snapshotSignal.set(null);
    this.persistSnapshot(null);
  }

  async refreshIdToken(): Promise<void> {
    const user = this.userSignal();
    if (!user) {
      return;
    }

    await this.syncSnapshot(user);
  }

  async waitUntilReady(): Promise<void> {
    if (this.readySignal()) {
      return;
    }

    await new Promise<void>((resolve) => {
      const interval = window.setInterval(() => {
        if (this.readySignal()) {
          window.clearInterval(interval);
          resolve();
        }
      }, 20);
    });
  }

  reportError(message: string): void {
    this.setError(message);
  }

  private bootstrap(): void {
    const config = FIREBASE_WEB_CONFIG;
    if (!this.isValidConfig(config)) {
      this.errorSignal.set('Set your Firebase web config in src/app/auth/app-config.ts.');
      this.readySignal.set(false);
      return;
    }

    const app = this.ensureApp(config);
    const auth = getAuth(app);
    this.authSignal.set(auth);

    if (this.authStateUnsubscribe) {
      this.authStateUnsubscribe();
    }

    this.authStateUnsubscribe = onAuthStateChanged(auth, async (user) => {
      this.userSignal.set(user);
      if (user) {
        await this.syncSnapshot(user);
      } else {
        this.snapshotSignal.set(null);
        this.persistSnapshot(null);
      }
      this.readySignal.set(true);
    });
  }

  private ensureApp(config: FirebaseWebConfig): FirebaseApp {
    const firebaseOptions: FirebaseOptions = {
      apiKey: config.apiKey,
      authDomain: config.authDomain,
      projectId: config.projectId,
      appId: config.appId,
    };

    if (getApps().length > 0) {
      return getApp();
    }

    return initializeApp(firebaseOptions);
  }

  private ensureAuth(): Auth {
    const auth = this.authSignal();
    if (!auth) {
      throw new Error('Firebase is not ready yet.');
    }

    return auth;
  }

  private async syncSnapshot(user: User): Promise<void> {
    const snapshot: AuthSnapshot = {
      email: user.email ?? '',
      firebaseUid: user.uid,
      idToken: await user.getIdToken(true),
      displayName: user.displayName ?? '',
    };

    this.snapshotSignal.set(snapshot);
    this.persistSnapshot(snapshot);
  }

  private persistSnapshot(snapshot: AuthSnapshot | null): void {
    if (snapshot) {
      localStorage.setItem(AUTH_SNAPSHOT_STORAGE_KEY, JSON.stringify(snapshot));
      return;
    }

    localStorage.removeItem(AUTH_SNAPSHOT_STORAGE_KEY);
  }

  private loadStoredSnapshot(): AuthSnapshot | null {
    const raw = localStorage.getItem(AUTH_SNAPSHOT_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthSnapshot;
    } catch {
      return null;
    }
  }

  private setError(message: string): void {
    this.errorSignal.set(message);
  }

  private isValidConfig(config: FirebaseWebConfig): boolean {
    return (
      !config.apiKey.startsWith('REPLACE_') &&
      !config.authDomain.startsWith('REPLACE_') &&
      !config.projectId.startsWith('REPLACE_') &&
      !config.appId.startsWith('REPLACE_')
    );
  }
}
