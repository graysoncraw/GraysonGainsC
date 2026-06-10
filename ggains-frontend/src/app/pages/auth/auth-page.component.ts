import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { FirebaseAuthService } from '../../auth/firebase-auth.service';

type AuthMode = 'login' | 'signup';

type AuthFormState = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

@Component({
  selector: 'app-auth-page',
  imports: [FormsModule],
  templateUrl: './auth-page.component.html',
  styleUrl: './auth-page.component.scss',
})
export class AuthPageComponent {
  protected readonly mode = signal<AuthMode>('login');
  protected readonly form = signal<AuthFormState>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });
  protected readonly busy = signal(false);
  protected readonly error = computed(() => this.firebaseAuth.error());
  protected readonly ready = computed(() => this.firebaseAuth.ready());
  protected readonly signedIn = computed(() => this.firebaseAuth.user() !== null);
  protected readonly isSignupMode = computed(() => this.mode() === 'signup');
  protected readonly submitLabel = computed(() => (this.isSignupMode() ? 'Create account' : 'Log in'));
  protected readonly helperText = computed(() =>
    this.isSignupMode()
      ? 'Create your account and start your GraysonGains profile.'
      : 'Log in to continue your training log.',
  );

  constructor(
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly router: Router,
  ) {
    effect(() => {
      if (this.ready() && this.signedIn()) {
        void this.router.navigateByUrl('/setup/profile');
      }
    });
  }

  protected setMode(mode: AuthMode): void {
    this.mode.set(mode);
  }

  protected updateField(field: keyof AuthFormState, value: string): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  protected async submit(): Promise<void> {
    this.busy.set(true);

    try {
      if (this.isSignupMode()) {
        const profile = this.profileDraft();
        await this.firebaseAuth.signUpWithEmail(profile.email, profile.password, `${profile.firstName} ${profile.lastName}`);
      } else {
        const email = this.form().email.trim();
        const password = this.form().password;
        await this.firebaseAuth.signInWithEmail(email, password);
      }

      await this.router.navigateByUrl('/setup/profile');
    } catch {
    } finally {
      this.busy.set(false);
    }
  }

  protected async signInWithGoogle(): Promise<void> {
    this.busy.set(true);

    try {
      await this.firebaseAuth.signInWithGoogle();
      await this.router.navigateByUrl('/setup/profile');
    } catch {
    } finally {
      this.busy.set(false);
    }
  }

  private profileDraft(): AuthFormState {
    const form = this.form();
    return {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      email: form.email.trim(),
      password: form.password,
    };
  }
}
