import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { mapApiProblem } from './core/api-problem';
import { Credentials, SignUpRequest, User } from './core/models';

/**
 * REST client for the EMA backend. Every method fails with an `ApiProblem` (never a raw
 * `HttpErrorResponse`), see `core/api-problem.ts`.
 *
 * URLs are relative so the browser stays on its own origin (dev-server proxy or nginx), which
 * also lets Angular's built-in XSRF support send `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/1.0';

  /** Opens a session: the backend answers with the user and sets an HttpOnly cookie. */
  login(credentials: Credentials): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/session`, credentials).pipe(mapApiProblem());
  }

  /** Returns the current user, or fails with an `UNAUTHENTICATED` problem (401). */
  currentUser(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/session`).pipe(mapApiProblem());
  }

  logout(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/session`).pipe(mapApiProblem());
  }

  /** Creates an account. The user object is the request body (no `{data: ...}` wrapper). */
  signup(user: SignUpRequest): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/users`, user).pipe(mapApiProblem());
  }
}
