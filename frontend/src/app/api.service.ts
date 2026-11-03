import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { mapApiProblem } from './core/api-problem';
import {
  Credentials,
  DataResponse,
  DenialReason,
  Mobility,
  MobilityChoice,
  MobilityChoiceFilter,
  MobilityChoiceRequest,
  MobilityDocumentFilter,
  NominatedStudent,
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
 * URL of the CSV export of the documents of the mobility `id` (opened by the browser itself), every
 * document without a filter.
 */
export function mobilityExportUrl(id: number, filter?: MobilityDocumentFilter): string {
  const query = filter ? `?${new HttpParams({ fromObject: { filter } }).toString()}` : '';
  return `${API_BASE_URL}/mobilities/${id}/export${query}`;
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
      .get<DataResponse<MobilityChoice>>(`${this.baseUrl}/mobilityChoices`, {
        params: filterParams(filter),
      })
      .pipe(
        map((response) => response.data),
        mapApiProblem(),
      );
  }

  /** Number of mobility choices `mobilityChoices(filter)` would return. */
  countMobilityChoices(filter?: MobilityChoiceFilter): Observable<number> {
    return this.http
      .get<{ count: number }>(`${this.baseUrl}/mobilityChoices/count`, {
        params: filterParams(filter),
      })
      .pipe(
        map((response) => response.count),
        mapApiProblem(),
      );
  }

  /** Creates a mobility choice (note the singular path of the API) and returns it with its id. */
  createMobilityChoice(choice: MobilityChoiceRequest): Observable<MobilityChoice> {
    return this.http
      .post<MobilityChoice>(`${this.baseUrl}/mobilityChoice`, choice)
      .pipe(mapApiProblem());
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
      .put<void>(`${this.baseUrl}/mobilityChoices/${id}/reject`, null, {
        params: { reason: denialReasonId },
      })
      .pipe(mapApiProblem());
  }

  /**
   * Ids of the mobilities the user may see (every one for a professor). A mobility has the id of
   * the choice it was confirmed from, which tells which choices of a list became mobilities.
   */
  mobilityIds(): Observable<number[]> {
    return this.http.get<DataResponse<{ id: number }>>(`${this.baseUrl}/mobilities`).pipe(
      map((response) => response.data.map((mobility) => mobility.id)),
      mapApiProblem(),
    );
  }

  /** Every mobility for a professor, the own ones for a student, unwrapped from `{data: [...]}`. */
  mobilities(): Observable<Mobility[]> {
    return this.http.get<DataResponse<Mobility>>(`${this.baseUrl}/mobilities`).pipe(
      map((response) => response.data),
      mapApiProblem(),
    );
  }

  /** One mobility with its documents and the student's data (a student may only read their own). */
  mobility(id: number): Observable<Mobility> {
    return this.http.get<Mobility>(`${this.baseUrl}/mobilities/${id}`).pipe(mapApiProblem());
  }

  /**
   * A professor records that the mobility is encoded in ProEco. Like every state change of a
   * mobility it takes the version last read: a stale one fails with a 409 problem.
   */
  confirmProEcoEncoding(id: number, version: number): Observable<void> {
    return this.mobilityChange(`${id}/confirmProEcoEncoding`, { version });
  }

  /** A professor records that the mobility is encoded in the software of its programme. */
  confirmSecondSoftwareEncoding(id: number, version: number): Observable<void> {
    return this.mobilityChange(`${id}/confirmSecondSoftwareEncoding`, { version });
  }

  /** A professor records that the document `documentId` of the mobility is filled in. */
  confirmMobilityDocument(id: number, documentId: number, version: number): Observable<void> {
    return this.mobilityChange(`${id}/confirmDocument`, { document: documentId, version });
  }

  /** A professor records the next payment (the first one in "A payer", the second in "Solde à payer"). */
  confirmMobilityPayment(id: number, version: number): Observable<void> {
    return this.mobilityChange(`${id}/confirmPayment`, { version });
  }

  /** A professor cancels the mobility with one of the denial reasons, given by id. */
  cancelMobility(id: number, version: number, denialReasonId: number): Observable<void> {
    return this.mobilityChange(`${id}/cancel`, { version, denialReason: denialReasonId });
  }

  /** A professor confirms a choice that has a partner: it becomes a mobility. */
  confirmMobilityChoice(id: number): Observable<void> {
    return this.http
      .put<void>(`${this.baseUrl}/mobilityChoices/${id}/confirm`, null)
      .pipe(mapApiProblem());
  }

  /**
   * The personal data of the nominated student having the user id `id` (a student may only read
   * their own). Fails with a `RESOURCE_NOT_FOUND` problem (404) until they were saved once.
   */
  nominatedStudent(id: number): Observable<NominatedStudent> {
    return this.http
      .get<NominatedStudent>(`${this.baseUrl}/nominatedStudents/${id}`)
      .pipe(mapApiProblem());
  }

  /** Saves the personal data of a student for the first time and returns them with their version. */
  createNominatedStudent(student: NominatedStudent): Observable<NominatedStudent> {
    return this.http
      .post<NominatedStudent>(`${this.baseUrl}/nominatedStudents`, student)
      .pipe(mapApiProblem());
  }

  /**
   * Updates the personal data of the student `student.id`. The version must be the one last read
   * (a stale one fails with a 409 `CONCURRENT_MODIFICATION` problem); the answer carries the new one.
   */
  updateNominatedStudent(student: NominatedStudent): Observable<NominatedStudent> {
    return this.http
      .put<NominatedStudent>(`${this.baseUrl}/nominatedStudents/${student.id}`, student)
      .pipe(mapApiProblem());
  }

  /** Every user account (professor only), unwrapped from `{data: [...]}`. */
  users(): Observable<User[]> {
    return this.http.get<DataResponse<User>>(`${this.baseUrl}/users`).pipe(
      map((response) => response.data),
      mapApiProblem(),
    );
  }

  /** A professor promotes the user `id` to professor (a professor is left unchanged). */
  promoteUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/users/${id}/promote`, null).pipe(mapApiProblem());
  }

  /**
   * `PUT /mobilities/{path}` without a body. Some of these answer the mobility, others nothing: the
   * screen reads it again in any case, so the answer is dropped.
   */
  private mobilityChange(path: string, params: Record<string, number>): Observable<void> {
    return this.http.put<unknown>(`${this.baseUrl}/mobilities/${path}`, null, { params }).pipe(
      map(() => undefined),
      mapApiProblem(),
    );
  }
}

function filterParams(filter: MobilityChoiceFilter | undefined): Record<string, string> {
  return filter ? { filter } : {};
}
