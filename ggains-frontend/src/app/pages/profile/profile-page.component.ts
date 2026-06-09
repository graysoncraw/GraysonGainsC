import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { OnboardingFlowService } from '../../auth/onboarding-flow.service';
import { SessionService } from '../../auth/session.service';
import { FirebaseAuthService } from '../../auth/firebase-auth.service';
import { UserApiService, UserProfileRequest } from '../../api/user-api.service';

type ProfileFormState = UserProfileRequest;

@Component({
  selector: 'app-profile-page',
  imports: [FormsModule],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
})
export class ProfilePageComponent {
  protected readonly form = signal<ProfileFormState>({
    firstName: '',
    lastName: '',
    email: '',
    gender: '',
    heightFt: null,
    heightIn: null,
    weight: null,
  });
  protected readonly loading = signal(false);
  protected readonly deleting = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly currentUser = computed(() => this.session.backendUser());
  protected readonly onboardingMode = signal(false);

  constructor(
    private readonly session: SessionService,
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly onboardingFlow: OnboardingFlowService,
    private readonly userApi: UserApiService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {
    this.onboardingMode.set(this.route.snapshot.data['onboarding'] === true);
    effect(() => {
      const user = this.currentUser();
      if (user) {
        this.form.set({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          gender: user.gender || '',
          heightFt: user.heightFt ?? null,
          heightIn: user.heightIn ?? null,
          weight: user.weight ?? null,
        });
        return;
      }

      if (this.onboardingMode()) {
        const snapshot = this.firebaseAuth.snapshot();
        const displayName = snapshot?.displayName?.trim() || '';
        const displayParts = displayName.split(/\s+/).filter(Boolean);
        const fallbackEmail = snapshot?.email || '';

        this.form.set({
          firstName: displayParts[0] || fallbackEmail.split('@')[0] || '',
          lastName: displayParts[1] || 'User',
          email: fallbackEmail,
          gender: '',
          heightFt: null,
          heightIn: null,
          weight: null,
        });
      }
    });
  }

  protected updateField<K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  protected updateNumberField(field: 'heightFt' | 'heightIn' | 'weight', value: string): void {
    this.form.update((current) => ({
      ...current,
      [field]: value === '' ? null : Number(value),
    }));
  }

  protected async save(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const saved = this.currentUser()
        ? await this.userApi.updateUser(firebaseUid, this.form())
        : await this.userApi.createUser(this.form());

      this.session.setBackendUser(saved);
      this.success.set('Profile saved.');

      if (this.onboardingMode()) {
        await this.router.navigateByUrl(await this.onboardingFlow.resolveRoute());
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to save profile.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async deleteAccount(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    const confirmed = window.confirm('Delete the backend user profile for this account?');
    if (!confirmed) {
      return;
    }

    this.deleting.set(true);
    this.error.set('');

    try {
      await this.userApi.deleteUser(firebaseUid);
      this.session.clearSessionState();
      await this.firebaseAuth.signOut();
      await this.router.navigateByUrl('/auth');
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to delete profile.');
    } finally {
      this.deleting.set(false);
    }
  }
}
