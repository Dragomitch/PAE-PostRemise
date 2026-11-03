import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem } from './api-problem';
import { Credentials, ROLE_PROFESSOR, User } from './models';

/**
 * The signed-in user. The session itself is an HttpOnly cookie the browser keeps, so the only
 * way to know who is signed in after a page load is `GET /api/1.0/session` (`restore()`); the
 * result is cached in `user` for the lifetime of the page. Nothing is kept in web storage.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly api = inject(ApiService);
  private readonly current = signal<User | null>(null);

  /** The signed-in user, `null` while anonymous (or before `restore()` answered). */
  readonly user = this.current.asReadonly();
  readonly isProfessor = computed(() => this.current()?.role === ROLE_PROFESSOR);

  /** Opens a session. A failure is an `ApiProblem` and leaves the state untouched. */
  signIn(credentials: Credentials): Observable<User> {
    return this.api.login(credentials).pipe(tap((user) => this.current.set(user)));
  }

  /**
   * Asks the backend who the cookie belongs to. Never fails: no cookie, an expired session or an
   * unreachable backend all mean "not signed in" (`null`), which sends the visitor to the
   * sign-in page, where a real outage is reported by the sign-in request itself.
   */
  restore(): Observable<User | null> {
    return this.api.currentUser().pipe(
      catchError(() => of(null)),
      tap((user) => this.current.set(user)),
    );
  }

  /**
   * Closes the session on the server, then forgets the user. A session the server no longer knows
   * (401) is already closed. Any other failure is re-thrown as an `ApiProblem` and the user stays
   * signed in, because the cookie may still be valid.
   */
  signOut(): Observable<void> {
    return this.api.logout().pipe(
      catchError((problem: ApiProblem) =>
        problem.status === 401 ? of(undefined) : throwError(() => problem),
      ),
      tap(() => this.current.set(null)),
      map(() => undefined),
    );
  }

  /** Forgets the user without calling the server (the server just said the session is gone). */
  clear(): void {
    this.current.set(null);
  }
}
