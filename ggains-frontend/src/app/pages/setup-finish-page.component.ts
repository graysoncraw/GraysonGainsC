import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';

import { OnboardingFlowService } from '../auth/onboarding-flow.service';

@Component({
  selector: 'app-setup-finish-page',
  templateUrl: './setup-finish-page.component.html',
  styleUrl: './setup-finish-page.component.scss',
})
export class SetupFinishPageComponent {
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  constructor(
    private readonly onboardingFlow: OnboardingFlowService,
    private readonly router: Router,
  ) {
    void this.refresh();
  }

  protected async refresh(): Promise<void> {
    this.loading.set(true);
    this.error.set('');

    try {
      const route = await this.onboardingFlow.resolveRoute();
      if (route === '/app') {
        await this.router.navigateByUrl('/app');
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to finish setup.');
    } finally {
      this.loading.set(false);
    }
  }
}
