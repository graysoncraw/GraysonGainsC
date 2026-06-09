import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { FirebaseAuthService } from './firebase-auth.service';

export const authGuard: CanActivateFn = async () => {
  const firebaseAuth = inject(FirebaseAuthService);
  const router = inject(Router);

  await firebaseAuth.waitUntilReady();
  return firebaseAuth.user() ? true : router.parseUrl('/auth');
};
