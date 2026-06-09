import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { BACKEND_API_BASE_URL } from './app-config';
import { FirebaseAuthService } from './firebase-auth.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(BACKEND_API_BASE_URL)) {
    return next(request);
  }

  const firebaseAuth = inject(FirebaseAuthService);
  const idToken = firebaseAuth.idToken();
  if (!idToken) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${idToken}`,
      },
    }),
  );
};
