import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { OnboardingFlowService } from '../onboarding-flow.service';

export const appGuard: CanActivateFn = async (_route, state) => {
  const flow = inject(OnboardingFlowService);
  const router = inject(Router);
  const nextRoute = await flow.resolveRoute();

  if (nextRoute === '/app') {
    return true;
  }

  return router.parseUrl(nextRoute);
};
