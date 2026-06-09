import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { OnboardingFlowService } from '../onboarding-flow.service';

export const guestGuard: CanActivateFn = async () => {
  const flow = inject(OnboardingFlowService);
  const router = inject(Router);

  const nextRoute = await flow.resolveRoute();
  return nextRoute === '/auth' ? true : router.parseUrl(nextRoute);
};
