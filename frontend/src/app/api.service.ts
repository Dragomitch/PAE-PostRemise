import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpEventType,
  HttpParams,
  HttpRequest,
  HttpResponse,
  type HttpEvent,
} from '@angular/common/http';
import { Observable, filter, map } from 'rxjs';

import { mapApiProblem } from './core/api-problem';
import {
  deleteSessionRequest,
  getCountriesRequest,
  getDenialReasonsRequest,
  getMobilitiesByIdRequest,
  getMobilitiesRequest,
  getMobilityChoicesCountRequest,
  getMobilityChoicesRequest,
  getNominatedStudentsByIdRequest,
  getPartnersRequest,
  getProgrammesRequest,
  getSessionRequest,
  getUsersRequest,
  postDenialReasonsRequest,
  postMobilityChoiceRequest,
  postNominatedStudentsRequest,
  postSessionRequest,
  postUsersRequest,
  putDenialReasonsByIdRequest,
  putMobilitiesByIdCancelRequest,
  putMobilitiesByIdConfirmDocumentRequest,
  putMobilitiesByIdConfirmPaymentRequest,
  putMobilitiesByIdConfirmProEcoEncodingRequest,
  putMobilitiesByIdConfirmSecondSoftwareEncodingRequest,
  putMobilityChoicesByIdCancelRequest,
  putMobilityChoicesByIdConfirmRequest,
  putMobilityChoicesByIdRejectRequest,
  putNominatedStudentsByIdRequest,
  putUsersByIdPromoteRequest,
} from './core/api/@angular/common.gen';
import { createClient } from './core/api/client';
import { client as urlClient } from './core/api/client.gen';
import type {
  GetMobilitiesByIdExportData,
  GetMobilityChoicesExportData,
  NominatedStudentDtoWritable,
  MobilityChoiceDtoWritable,
} from './core/api';
import {
  Credentials,
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
  return urlClient.buildUrl<GetMobilityChoicesExportData>({
    url: '/api/1.0/mobilityChoices/export',
    query: { filter },
  });
}

/**
 * URL of the CSV export of the documents of the mobility `id` (opened by the browser itself), every
 * document without a filter.
 */
export function mobilityExportUrl(id: number, filter?: MobilityDocumentFilter): string {
  return urlClient.buildUrl<GetMobilitiesByIdExportData>({
    url: '/api/1.0/mobilities/{id}/export',
    path: { id },
    query: filter ? { filter } : undefined,
  });
}

/**
 * REST client for the EMA backend. Every method fails with an `ApiProblem` (never a raw
 * `HttpErrorResponse`), see `core/api-problem.ts`.
 *
 * The requests are built by the client generated from the OpenAPI document (`core/api/`, the
 * `<operation>Request` functions of `npm run api:generate`): their paths, parameters and bodies
 * are checked against the document at compile time. They are sent with the application's
 * `HttpClient`, so the interceptors apply (Accept-Language, XSRF) and the errors are
 * `HttpErrorResponse`s that `mapApiProblem()` turns into problems. The answers are typed with the
 * models of `core/models.ts`, derived from the generated schema types.
 *
 * URLs are relative so the browser stays on its own origin (dev-server proxy or nginx), which
 * also lets Angular's built-in XSRF support send `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  /**
   * Builds the requests only (no base URL: relative paths). Its `HttpClient` is the application's,
   * which it would also use to send them; `send()` does that itself to keep the Observables.
   */
  private readonly client = createClient({ httpClient: this.http });

  /** Opens a session: the backend answers with the user and sets an HttpOnly cookie. */
  login(credentials: Credentials): Observable<User> {
    return this.send(
      postSessionRequest({ client: this.client, body: credentials }),
      (body) => body as User,
    );
  }

  /** Returns the current user, or fails with an `UNAUTHENTICATED` problem (401). */
  currentUser(): Observable<User> {
    return this.send(getSessionRequest({ client: this.client }), (body) => body as User);
  }

  logout(): Observable<void> {
    return this.send(deleteSessionRequest({ client: this.client }), () => undefined);
  }

  /** Creates an account. The user object is the request body (no `{data: ...}` wrapper). */
  signup(user: SignUpRequest): Observable<User> {
    return this.send(postUsersRequest({ client: this.client, body: user }), (body) => body as User);
  }

  /**
   * The partners the user may see: every partner for a professor (archived ones included), the
   * official partners of the student's option for a student.
   */
  partners(): Observable<Partner[]> {
    return this.send(
      getPartnersRequest({ client: this.client }),
      (response) => response.data as Partner[],
    );
  }

  /** Mobility programmes (Erasmus+, Erabel, FAME). The API returns a plain array. */
  programmes(): Observable<Programme[]> {
    return this.send(getProgrammesRequest({ client: this.client }), (body) => body as Programme[]);
  }

  /** Every country, with the programme its mobilities belong to. The API returns a plain array. */
  countries(): Observable<ProgrammeCountry[]> {
    return this.send(
      getCountriesRequest({ client: this.client }),
      (body) => body as ProgrammeCountry[],
    );
  }

  /** Reasons a professor can give to reject a mobility choice. The API returns a plain array. */
  denialReasons(): Observable<DenialReason[]> {
    return this.send(
      getDenialReasonsRequest({ client: this.client }),
      (body) => body as DenialReason[],
    );
  }

  /** A professor creates a denial reason (300 characters at most); the answer carries its id. */
  createDenialReason(reason: string): Observable<DenialReason> {
    return this.send(
      postDenialReasonsRequest({ client: this.client, body: { reason } }),
      (body) => body as DenialReason,
    );
  }

  /**
   * A professor changes the text of the denial reason `denialReason.id`, sending the version last
   * read: a reason that no longer exists fails with a 409 `CONCURRENT_MODIFICATION` problem.
   */
  updateDenialReason(denialReason: DenialReason): Observable<DenialReason> {
    return this.send(
      putDenialReasonsByIdRequest({
        client: this.client,
        path: { id: denialReason.id },
        body: denialReason,
      }),
      (body) => body as DenialReason,
    );
  }

  /**
   * The mobility choices of the signed-in student, or, for a professor, every choice matching the
   * filter (without filter the API applies `active`). The API ignores the filter for a student.
   */
  mobilityChoices(filter?: MobilityChoiceFilter): Observable<MobilityChoice[]> {
    return this.send(
      getMobilityChoicesRequest({ client: this.client, query: filterQuery(filter) }),
      (response) => response.data as MobilityChoice[],
    );
  }

  /** Number of mobility choices `mobilityChoices(filter)` would return. */
  countMobilityChoices(filter?: MobilityChoiceFilter): Observable<number> {
    return this.send(
      getMobilityChoicesCountRequest({ client: this.client, query: filterQuery(filter) }),
      (response) => response.count as number,
    );
  }

  /** Creates a mobility choice (note the singular path of the API) and returns it with its id. */
  createMobilityChoice(choice: MobilityChoiceRequest): Observable<MobilityChoice> {
    return this.send(
      postMobilityChoiceRequest({
        client: this.client,
        // references by id: the document asks for the whole user, programme, country and partner
        body: choice as unknown as MobilityChoiceDtoWritable,
      }),
      (body) => body as MobilityChoice,
    );
  }

  /** The student cancels one of their choices; the reason is a query parameter (500 characters at most). */
  cancelMobilityChoice(id: number, reason: string): Observable<void> {
    return this.send(
      putMobilityChoicesByIdCancelRequest({ client: this.client, path: { id }, query: { reason } }),
      () => undefined,
    );
  }

  /** A professor rejects a choice with one of the denial reasons, given by id. */
  rejectMobilityChoice(id: number, denialReasonId: number): Observable<void> {
    return this.send(
      putMobilityChoicesByIdRejectRequest({
        client: this.client,
        path: { id },
        query: { reason: denialReasonId },
      }),
      () => undefined,
    );
  }

  /**
   * Ids of the mobilities the user may see (every one for a professor). A mobility has the id of
   * the choice it was confirmed from, which tells which choices of a list became mobilities.
   */
  mobilityIds(): Observable<number[]> {
    return this.send(getMobilitiesRequest({ client: this.client }), (response) =>
      (response.data as Mobility[]).map((mobility) => mobility.id),
    );
  }

  /** Every mobility for a professor, the own ones for a student, unwrapped from `{data: [...]}`. */
  mobilities(): Observable<Mobility[]> {
    return this.send(
      getMobilitiesRequest({ client: this.client }),
      (response) => response.data as Mobility[],
    );
  }

  /** One mobility with its documents and the student's data (a student may only read their own). */
  mobility(id: number): Observable<Mobility> {
    return this.send(
      getMobilitiesByIdRequest({ client: this.client, path: { id } }),
      (body) => body as Mobility,
    );
  }

  /**
   * A professor records that the mobility is encoded in ProEco. Like every state change of a
   * mobility it takes the version last read: a stale one fails with a 409 problem.
   *
   * Some of these changes answer the mobility, others nothing: the screen reads it again in any
   * case, so the answer is dropped.
   */
  confirmProEcoEncoding(id: number, version: number): Observable<void> {
    return this.send(
      putMobilitiesByIdConfirmProEcoEncodingRequest({
        client: this.client,
        path: { id },
        query: { version },
      }),
      () => undefined,
    );
  }

  /** A professor records that the mobility is encoded in the software of its programme. */
  confirmSecondSoftwareEncoding(id: number, version: number): Observable<void> {
    return this.send(
      putMobilitiesByIdConfirmSecondSoftwareEncodingRequest({
        client: this.client,
        path: { id },
        query: { version },
      }),
      () => undefined,
    );
  }

  /** A professor records that the document `documentId` of the mobility is filled in. */
  confirmMobilityDocument(id: number, documentId: number, version: number): Observable<void> {
    return this.send(
      putMobilitiesByIdConfirmDocumentRequest({
        client: this.client,
        path: { id },
        query: { document: documentId, version },
      }),
      () => undefined,
    );
  }

  /** A professor records the next payment (the first one in "A payer", the second in "Solde à payer"). */
  confirmMobilityPayment(id: number, version: number): Observable<void> {
    return this.send(
      putMobilitiesByIdConfirmPaymentRequest({
        client: this.client,
        path: { id },
        query: { version },
      }),
      () => undefined,
    );
  }

  /** A professor cancels the mobility with one of the denial reasons, given by id. */
  cancelMobility(id: number, version: number, denialReasonId: number): Observable<void> {
    return this.send(
      putMobilitiesByIdCancelRequest({
        client: this.client,
        path: { id },
        query: { version, denialReason: denialReasonId },
      }),
      () => undefined,
    );
  }

  /** A professor confirms a choice that has a partner: it becomes a mobility. */
  confirmMobilityChoice(id: number): Observable<void> {
    return this.send(
      putMobilityChoicesByIdConfirmRequest({ client: this.client, path: { id } }),
      () => undefined,
    );
  }

  /**
   * The personal data of the nominated student having the user id `id` (a student may only read
   * their own). Fails with a `RESOURCE_NOT_FOUND` problem (404) until they were saved once.
   */
  nominatedStudent(id: number): Observable<NominatedStudent> {
    return this.send(
      getNominatedStudentsByIdRequest({ client: this.client, path: { id } }),
      (body) => body as NominatedStudent,
    );
  }

  /** Saves the personal data of a student for the first time and returns them with their version. */
  createNominatedStudent(student: NominatedStudent): Observable<NominatedStudent> {
    return this.send(
      postNominatedStudentsRequest({ client: this.client, body: studentBody(student) }),
      (body) => body as NominatedStudent,
    );
  }

  /**
   * Updates the personal data of the student `student.id`. The version must be the one last read
   * (a stale one fails with a 409 `CONCURRENT_MODIFICATION` problem); the answer carries the new one.
   */
  updateNominatedStudent(student: NominatedStudent): Observable<NominatedStudent> {
    return this.send(
      putNominatedStudentsByIdRequest({
        client: this.client,
        path: { id: student.id },
        body: studentBody(student),
      }),
      (body) => body as NominatedStudent,
    );
  }

  /** Every user account (professor only), unwrapped from `{data: [...]}`. */
  users(): Observable<User[]> {
    return this.send(
      getUsersRequest({ client: this.client }),
      (response) => response.data as User[],
    );
  }

  /** A professor promotes the user `id` to professor (a professor is left unchanged). */
  promoteUser(id: number): Observable<void> {
    return this.send(
      putUsersByIdPromoteRequest({ client: this.client, path: { id } }),
      () => undefined,
    );
  }

  /**
   * Sends a request built by the generated client and answers `project` of its JSON body. The
   * generated functions declare the response type of the document as the type argument of the
   * `HttpRequest` (`R`); `project` unwraps it or narrows it to the model of `core/models.ts` (a
   * type assertion, which TypeScript refuses when the two types have nothing in common).
   */
  private send<R, M>(request: HttpRequest<R>, project: (body: R) => M): Observable<M> {
    return this.http.request<R>(withParams(request)).pipe(
      filter(
        (event: HttpEvent<R>): event is HttpResponse<R> => event.type === HttpEventType.Response,
      ),
      map((response) => project(response.body as R)),
      mapApiProblem(),
    );
  }
}

/**
 * The request with the query string that the generated client wrote into the URL moved to its
 * `params`, where Angular keeps the parameters of a request (`url` stays the path; `urlWithParams`
 * is unchanged).
 */
function withParams<R>(request: HttpRequest<R>): HttpRequest<R> {
  const start = request.url.indexOf('?');
  if (start < 0) {
    return request;
  }
  return request.clone({
    url: request.url.substring(0, start),
    params: new HttpParams({ fromString: request.url.substring(start + 1) }),
  });
}

function filterQuery(filter: MobilityChoiceFilter | undefined): { filter?: string } | undefined {
  return filter ? { filter } : undefined;
}

/**
 * The record as the API reads it: the nationality and the address country by code, and the region
 * and holder `null` when empty, where the document asks for whole countries and strings.
 */
function studentBody(student: NominatedStudent): NominatedStudentDtoWritable {
  return student as unknown as NominatedStudentDtoWritable;
}
