import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, firstValueFrom, of } from 'rxjs';

/** Cookie set by Spring Security's CookieCsrfTokenRepository, read by HttpClient's XSRF support. */
export const XSRF_COOKIE = 'XSRF-TOKEN';

/** Public endpoint: any API response carries the XSRF-TOKEN cookie (backend CsrfCookieFilter). */
const CSRF_BOOTSTRAP_URL = '/api/1.0/options';

export function hasXsrfCookie(cookies: string): boolean {
  return cookies.split(';').some((c) => c.trim().startsWith(`${XSRF_COOKIE}=`));
}

/**
 * App initializer. The app is served by nginx, so on a fresh browser no API response has set the
 * XSRF-TOKEN cookie yet and the first POST (sign-in, sign-up) would be rejected with 403. One GET
 * to a public endpoint obtains it. Never blocks start-up: a failure is ignored (the next API
 * response sets the cookie anyway).
 */
export function initCsrfToken(doc: Pick<Document, 'cookie'> = document): Promise<unknown> {
  if (hasXsrfCookie(doc.cookie)) {
    return Promise.resolve();
  }
  const http = inject(HttpClient);
  return firstValueFrom(http.get(CSRF_BOOTSTRAP_URL).pipe(catchError(() => of(null))));
}
