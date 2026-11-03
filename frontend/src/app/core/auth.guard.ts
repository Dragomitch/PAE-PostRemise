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

/**
 * Lets through the users having `role` and sends the others to `/partners`, the screen of every
 * role. For the children of the shell route, whose `authGuard` already made sure the user is known.
 */
export function roleGuard(role: string): CanActivateFn {
  return () => {
    const router = inject(Router);
    return inject(SessionService).user()?.role === role || router.createUrlTree(['/partners']);
  };
}
