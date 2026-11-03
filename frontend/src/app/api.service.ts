import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { mapApiProblem } from './core/api-problem';
import {
  Credentials,
  DataResponse,
  DenialReason,
  MobilityChoice,
  MobilityChoiceFilter,
  MobilityChoiceRequest,
  Partner,
  Programme,
  ProgrammeCountry,
  SignUpRequest,
  User,
} from './core/models';

/** Base path of the REST API, relative to the origin that served the app. */
export const API_BASE_URL = '/api/1.0';

/** URL of the CSV export of the mobility choices matching the filter (opened by the browser itself). */
export function mobilityChoicesExportUrl(filter: MobilityChoiceFilter): string {
  return `${API_BASE_URL}/mobilityChoices/export?${new HttpParams({ fromObject: { filter } }).toString()}`;
}

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
  private readonly baseUrl = API_BASE_URL;

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

  /**
   * The partners the user may see: every partner for a professor (archived ones included), the
   * official partners of the student's option for a student.
   */
  partners(): Observable<Partner[]> {
    return this.http.get<DataResponse<Partner>>(`${this.baseUrl}/partners`).pipe(
      map((response) => response.data),
      mapApiProblem(),
    );
  }

  /** Mobility programmes (Erasmus+, Erabel, FAME). The API returns a plain array. */
  programmes(): Observable<Programme[]> {
    return this.http.get<Programme[]>(`${this.baseUrl}/programmes`).pipe(mapApiProblem());
  }

  /** Every country, with the programme its mobilities belong to. The API returns a plain array. */
  countries(): Observable<ProgrammeCountry[]> {
    return this.http.get<ProgrammeCountry[]>(`${this.baseUrl}/countries`).pipe(mapApiProblem());
  }

  /** Reasons a professor can give to reject a mobility choice. The API returns a plain array. */
  denialReasons(): Observable<DenialReason[]> {
    return this.http.get<DenialReason[]>(`${this.baseUrl}/denialReasons`).pipe(mapApiProblem());
  }

  /**
   * The mobility choices of the signed-in student, or, for a professor, every choice matching the
   * filter (without filter the API applies `active`). The API ignores the filter for a student.
   */
  mobilityChoices(filter?: MobilityChoiceFilter): Observable<MobilityChoice[]> {
    return this.http
      .get<DataResponse<MobilityChoice>>(`${this.baseUrl}/mobilityChoices`, { params: filterParams(filter) })
      .pipe(
        map((response) => response.data),
        mapApiProblem(),
      );
  }

  /** Number of mobility choices `mobilityChoices(filter)` would return. */
  countMobilityChoices(filter?: MobilityChoiceFilter): Observable<number> {
    return this.http
      .get<{ count: number }>(`${this.baseUrl}/mobilityChoices/count`, { params: filterParams(filter) })
      .pipe(
        map((response) => response.count),
        mapApiProblem(),
      );
  }

  /** Creates a mobility choice (note the singular path of the API) and returns it with its id. */
  createMobilityChoice(choice: MobilityChoiceRequest): Observable<MobilityChoice> {
    return this.http.post<MobilityChoice>(`${this.baseUrl}/mobilityChoice`, choice).pipe(mapApiProblem());
  }

  /** The student cancels one of their choices; the reason is a query parameter (500 characters at most). */
  cancelMobilityChoice(id: number, reason: string): Observable<void> {
    return this.http
      .put<void>(`${this.baseUrl}/mobilityChoices/${id}/cancel`, null, { params: { reason } })
      .pipe(mapApiProblem());
  }

  /** A professor rejects a choice with one of the denial reasons, given by id. */
  rejectMobilityChoice(id: number, denialReasonId: number): Observable<void> {
    return this.http
      .put<void>(`${this.baseUrl}/mobilityChoices/${id}/reject`, null, { params: { reason: denialReasonId } })
      .pipe(mapApiProblem());
  }

  /** A professor confirms a choice that has a partner: it becomes a mobility. */
  confirmMobilityChoice(id: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/mobilityChoices/${id}/confirm`, null).pipe(mapApiProblem());
  }
}

function filterParams(filter: MobilityChoiceFilter | undefined): Record<string, string> {
  return filter ? { filter } : {};
}
