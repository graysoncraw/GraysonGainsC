import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';

import { BackendUserSyncService } from './auth/backend-user-sync.service';
import { FirebaseAuthService } from './auth/firebase-auth.service';

type AuthFormState = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

@Component({
  selector: 'app-root',
  imports: [FormsModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly authMode = signal<'login' | 'signup'>('login');
  protected readonly form = signal<AuthFormState>({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  });

  protected readonly user = computed(() => this.firebaseAuth.user());
  protected readonly snapshot = computed(() => this.firebaseAuth.snapshot());
  protected readonly ready = computed(() => this.firebaseAuth.ready());
  protected readonly error = computed(() => this.firebaseAuth.error());
  protected readonly isSignedIn = computed(() => this.user() !== null);
  protected readonly signedInEmail = computed(() => this.snapshot()?.email || '');
  protected readonly signedInUid = computed(() => this.snapshot()?.firebaseUid || '');
  protected readonly isSignupMode = computed(() => this.authMode() === 'signup');
  protected readonly submitLabel = computed(() => (this.isSignupMode() ? 'Create account' : 'Log in'));

  constructor(
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly backendUserSync: BackendUserSyncService,
  ) {}

  protected setAuthMode(mode: 'login' | 'signup'): void {
    this.authMode.set(mode);
  }

  protected async signUp(): Promise<void> {
    try {
      await this.firebaseAuth.signUpWithEmail(this.form().email.trim(), this.form().password);
      await this.syncBackendProfile();
    } catch (error) {
      this.handleError(error);
    }
  }

  protected async signIn(): Promise<void> {
    try {
      await this.firebaseAuth.signInWithEmail(this.form().email.trim(), this.form().password);
      await this.syncBackendProfile();
    } catch (error) {
      this.handleError(error);
    }
  }

  protected async signInWithGoogle(): Promise<void> {
    try {
      await this.firebaseAuth.signInWithGoogle();
      await this.syncBackendProfile();
    } catch (error) {
      this.handleError(error);
    }
  }

  protected async signOut(): Promise<void> {
    try {
      await this.firebaseAuth.signOut();
    } catch (error) {
      this.handleError(error);
    }
  }

  protected async refreshSession(): Promise<void> {
    try {
      await this.firebaseAuth.refreshIdToken();
      await this.syncBackendProfile();
    } catch (error) {
      this.handleError(error);
    }
  }

  protected updateField(field: keyof AuthFormState, value: string): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  private async syncBackendProfile(): Promise<void> {
    const snapshot = this.snapshot();
    if (!snapshot) {
      return;
    }

    const profile = this.buildProfile(snapshot.email);

    await this.backendUserSync.ensureUser(snapshot.firebaseUid, snapshot.idToken, profile);
  }

  private buildProfile(fallbackEmail: string): { firstName: string; lastName: string; email: string } {
    const form = this.form();
    const email = form.email.trim() || fallbackEmail;
    const displayName = this.snapshot()?.displayName?.trim() || '';
    const displayParts = displayName.split(/\s+/).filter(Boolean);
    const derivedFirstName = displayParts[0] || email.split('@')[0] || '';
    const derivedLastName = displayParts[1] || 'User';

    return {
      firstName: form.firstName.trim() || derivedFirstName,
      lastName: form.lastName.trim() || derivedLastName,
      email,
    };
  }

  private handleError(error: unknown): void {
    const message = error instanceof Error ? error.message : 'Unknown error';
    this.firebaseAuth.reportError(message);
  }
}
