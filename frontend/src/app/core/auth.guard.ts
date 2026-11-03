import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { SessionService } from './session.service';

/**
 * Lets signed-in users through and sends anonymous visitors to `/signin`. The user is known
 * from this page load (sign-in) or from `GET /api/1.0/session`, so a reload keeps the session.
 */
export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);
  if (session.user()) {
    return true;
  }
  return session.restore().pipe(map((user) => (user ? true : router.createUrlTree(['/signin']))));
};
