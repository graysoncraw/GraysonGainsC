import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { SessionService } from '../auth/session.service';
import { FirebaseAuthService } from '../auth/firebase-auth.service';
import { UserApiService, UserProfileResponse } from '../api/user-api.service';

@Component({
  selector: 'app-settings-page',
  imports: [FormsModule],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss',
})
export class SettingsPageComponent {
  protected readonly status = signal('');
  protected readonly loading = signal(false);
  protected readonly lookupEmail = signal('');
  protected readonly lookupResult = signal<UserProfileResponse | null>(null);
  protected readonly lookupError = signal('');
  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly backendUser = computed(() => this.session.backendUser());

  constructor(
    private readonly session: SessionService,
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly userApi: UserApiService,
    private readonly router: Router,
  ) {}

  protected async refresh(): Promise<void> {
    this.loading.set(true);
    this.status.set('');

    try {
      await this.session.bootstrapCurrentSession();
      this.status.set('Session refreshed.');
    } catch (error) {
      this.status.set(error instanceof Error ? error.message : 'Unable to refresh session.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async signOut(): Promise<void> {
    await this.firebaseAuth.signOut();
    this.session.clearSessionState();
    await this.router.navigateByUrl('/auth');
  }

  protected async lookupByEmail(): Promise<void> {
    const email = this.lookupEmail().trim();
    if (!email) {
      this.lookupResult.set(null);
      this.lookupError.set('Enter an email first.');
      return;
    }

    this.lookupError.set('');
    this.lookupResult.set(null);

    try {
      this.lookupResult.set(await this.userApi.getUserByEmail(email));
    } catch (error) {
      this.lookupError.set(error instanceof Error ? error.message : 'Unable to look up user.');
    }
  }
}
