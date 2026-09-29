import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TimeoutError, of } from 'rxjs';

import { ApiProblem, ClientProblemCode, knownCodeMessage, mapApiProblem, statusFallback, toApiProblem } from './api-problem';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };

describe('toApiProblem (through HttpClient)', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  /** Performs a GET through mapApiProblem() and returns the emitted problem. */
  function failWith(respond: (req: ReturnType<HttpTestingController['expectOne']>) => void): ApiProblem {
    let problem: ApiProblem | undefined;
    http
      .get('/api/1.0/thing')
      .pipe(mapApiProblem())
      .subscribe({ next: () => fail('should fail'), error: (e: ApiProblem) => (problem = e) });
    respond(controller.expectOne('/api/1.0/thing'));
    expect(problem).withContext('an ApiProblem is emitted').toBeInstanceOf(ApiProblem);
    return problem!;
  }

  it('uses a valid backend problem as is', () => {
    const problem = failWith((req) =>
      req.flush(
        {
          type: 'urn:pae:problem:username-taken',
          title: 'Conflit',
          status: 409,
          detail: 'Le nom « jdoe » est déjà pris.',
          instance: '/api/1.0/users',
          code: 'USERNAME_TAKEN',
        },
        { status: 409, statusText: 'Conflict', headers: PROBLEM_HEADERS },
      ),
    );
    expect(problem.fromServer).toBeTrue();
    expect(problem.status).toBe(409);
    expect(problem.code).toBe('USERNAME_TAKEN');
    expect(problem.type).toBe('urn:pae:problem:username-taken');
    expect(problem.title).toBe('Conflit');
    expect(problem.detail).toBe('Le nom « jdoe » est déjà pris.');
    expect(problem.instance).toBe('/api/1.0/users');
    expect(problem.errors).toEqual([]);
    expect(problem.fieldErrors).toEqual({});
    expect(problem.fieldError('username')).toBeNull();
  });

  it('keeps the field errors of a validation problem, grouped by field', () => {
    const problem = failWith((req) =>
      req.flush(
        {
          type: 'urn:pae:problem:validation-failed',
          title: 'Données invalides',
          status: 400,
          detail: 'La requête contient des erreurs.',
          code: 'VALIDATION_FAILED',
          errors: [
            { field: 'email', code: 'Email', message: 'Adresse invalide' },
            { field: 'email', code: 'Size', message: 'Trop longue' },
            { field: 'username', code: 'NotBlank', message: 'Obligatoire' },
            { field: 42, message: 'ignored: invalid entry' },
          ],
        },
        { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
      ),
    );
    expect(problem.code).toBe('VALIDATION_FAILED');
    expect(problem.errors.length).toBe(3);
    expect(problem.fieldErrors).toEqual({ email: ['Adresse invalide', 'Trop longue'], username: ['Obligatoire'] });
    expect(problem.fieldError('email')).toBe('Adresse invalide Trop longue');
    expect(problem.fieldError('password')).toBeNull();
  });

  it('falls back to the known-code message when detail is missing', () => {
    const problem = failWith((req) =>
      req.flush(
        { title: 'Unauthorized', status: 401, code: 'INVALID_CREDENTIALS' },
        { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
      ),
    );
    expect(problem.fromServer).toBeTrue();
    expect(problem.detail).toBe(knownCodeMessage('INVALID_CREDENTIALS')!);
    expect(problem.title).toBe('Unauthorized');
  });

  it('falls back to the title, then to the status message, when detail and code are unusable', () => {
    const withTitle = failWith((req) =>
      req.flush({ title: 'Erreur métier', code: 'SOMETHING_NEW' }, { status: 422, statusText: 'Unprocessable' }),
    );
    expect(withTitle.detail).toBe('Erreur métier');
    expect(withTitle.code).toBe('SOMETHING_NEW');

    const withoutTitle = failWith((req) =>
      req.flush({ type: 'about:blank', detail: '   ' }, { status: 503, statusText: 'Unavailable' }),
    );
    expect(withoutTitle.code).toBe(ClientProblemCode.SERVICE_UNAVAILABLE);
    expect(withoutTitle.detail).toBe(statusFallback(503).message);
    expect(withoutTitle.title).toBe(withoutTitle.detail);
    expect(withoutTitle.type).toBe('about:blank');
  });

  it('never displays markup or stack traces coming from a problem body', () => {
    const problem = failWith((req) =>
      req.flush(
        {
          code: 'UNEXPECTED',
          title: '<b>Oops</b>',
          detail: 'java.lang.IllegalStateException: boom\n    at com.example.Foo.bar(Foo.java:1)',
        },
        { status: 500, statusText: 'Server Error', headers: PROBLEM_HEADERS },
      ),
    );
    expect(problem.detail).toBe(statusFallback(500).message);
    expect(problem.title).toBe(problem.detail);
  });

  it('maps a network failure (status 0) to NETWORK_ERROR', () => {
    const problem = failWith((req) => req.error(new ProgressEvent('error')));
    expect(problem.fromServer).toBeFalse();
    expect(problem.status).toBe(0);
    expect(problem.code).toBe(ClientProblemCode.NETWORK_ERROR);
    expect(problem.detail).toBe(statusFallback(0).message);
  });

  it('maps a proxy HTML error page (502) to SERVICE_UNAVAILABLE without exposing the HTML', () => {
    const problem = failWith((req) =>
      req.flush('<html><body><h1>502 Bad Gateway</h1></body></html>', {
        status: 502,
        statusText: 'Bad Gateway',
        headers: { 'Content-Type': 'text/html' },
      }),
    );
    expect(problem.code).toBe(ClientProblemCode.SERVICE_UNAVAILABLE);
    expect(problem.status).toBe(502);
    expect(problem.detail).not.toContain('<');
  });

  it('maps a proxy timeout page (504) to TIMEOUT', () => {
    const problem = failWith((req) =>
      req.flush('<html>504 Gateway Time-out</html>', { status: 504, statusText: 'Gateway Timeout' }),
    );
    expect(problem.code).toBe(ClientProblemCode.TIMEOUT);
  });

  it('maps a non-problem JSON 500 body to SERVER_ERROR', () => {
    const problem = failWith((req) =>
      req.flush(
        { timestamp: '2026-01-01', error: 'Internal Server Error', trace: 'java.lang...' },
        { status: 500, statusText: 'Server Error' },
      ),
    );
    expect(problem.code).toBe(ClientProblemCode.SERVER_ERROR);
    expect(problem.fromServer).toBeFalse();
    expect(problem.detail).not.toContain('java');
  });

  it('maps 401 and 403 without a problem body to UNAUTHENTICATED and ACCESS_DENIED', () => {
    const unauthenticated = failWith((req) => req.flush(null, { status: 401, statusText: 'Unauthorized' }));
    expect(unauthenticated.code).toBe(ClientProblemCode.UNAUTHENTICATED);
    expect(unauthenticated.detail).toBe(knownCodeMessage('UNAUTHENTICATED')!);

    const denied = failWith((req) => req.flush('Forbidden', { status: 403, statusText: 'Forbidden' }));
    expect(denied.code).toBe(ClientProblemCode.ACCESS_DENIED);
    expect(denied.detail).toBe(knownCodeMessage('ACCESS_DENIED')!);
  });

  it('uses the backend problem for 401/403 when there is one', () => {
    const problem = failWith((req) =>
      req.flush(
        { code: 'ACCESS_DENIED', detail: 'Réservé aux professeurs.', status: 403 },
        { status: 403, statusText: 'Forbidden', headers: PROBLEM_HEADERS },
      ),
    );
    expect(problem.fromServer).toBeTrue();
    expect(problem.detail).toBe('Réservé aux professeurs.');
  });
});

describe('toApiProblem (direct)', () => {
  it('parses a problem+json body delivered as text', () => {
    const problem = toApiProblem(
      new HttpErrorResponse({
        status: 409,
        error: JSON.stringify({ code: 'EMAIL_TAKEN', status: 409 }),
        headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
      }),
    );
    expect(problem.fromServer).toBeTrue();
    expect(problem.code).toBe('EMAIL_TAKEN');
    expect(problem.detail).toBe(knownCodeMessage('EMAIL_TAKEN')!);
  });

  it('ignores unparsable JSON text and text bodies without a JSON content type', () => {
    const broken = toApiProblem(
      new HttpErrorResponse({
        status: 500,
        error: '{"code": "BROKEN"',
        headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
      }),
    );
    expect(broken.code).toBe(ClientProblemCode.SERVER_ERROR);

    const plain = toApiProblem(new HttpErrorResponse({ status: 500, error: '{"code": "X"}' }));
    expect(plain.code).toBe(ClientProblemCode.SERVER_ERROR);
  });

  it('uses the status of the problem body when the HTTP status is unknown', () => {
    const problem = toApiProblem(new HttpErrorResponse({ status: 0, error: { code: 'X', status: 418 } }));
    expect(problem.status).toBe(418);
    const noStatus = toApiProblem(new HttpErrorResponse({ status: 0, error: { code: 'X' } }));
    expect(noStatus.status).toBe(0);
    expect(noStatus.detail).toBe(statusFallback(0).message);
  });

  it('derives the code from the status when the problem has none', () => {
    const problem = toApiProblem(new HttpErrorResponse({ status: 404, error: { title: 'Introuvable' } }));
    expect(problem.code).toBe(ClientProblemCode.NOT_FOUND);
    expect(problem.detail).toBe('Introuvable');
  });

  it('maps timeouts to TIMEOUT', () => {
    expect(toApiProblem(new TimeoutError()).code).toBe(ClientProblemCode.TIMEOUT);
    expect(toApiProblem({ name: 'TimeoutError' }).code).toBe(ClientProblemCode.TIMEOUT);
    const httpTimeout = toApiProblem(
      new HttpErrorResponse({ status: 0, error: new DOMException('timed out', 'TimeoutError') }),
    );
    expect(httpTimeout.code).toBe(ClientProblemCode.TIMEOUT);
    expect(httpTimeout.status).toBe(0);
  });

  it('maps anything else to UNKNOWN', () => {
    for (const error of [new Error('boom'), 'boom', null, undefined, { name: 42 }]) {
      const problem = toApiProblem(error);
      expect(problem.code).withContext(String(error)).toBe(ClientProblemCode.UNKNOWN);
      expect(problem.status).toBe(0);
      expect(problem.fromServer).toBeFalse();
    }
  });

  it('returns an ApiProblem unchanged', () => {
    const problem = toApiProblem(new Error('x'));
    expect(toApiProblem(problem)).toBe(problem);
  });

  it('leaves successful streams untouched', (done) => {
    of(1)
      .pipe(mapApiProblem())
      .subscribe((value) => {
        expect(value).toBe(1);
        done();
      });
  });
});

describe('statusFallback', () => {
  it('categorises statuses', () => {
    const cases: [number, string][] = [
      [0, 'NETWORK_ERROR'],
      [400, 'UNKNOWN'],
      [401, 'UNAUTHENTICATED'],
      [403, 'ACCESS_DENIED'],
      [404, 'NOT_FOUND'],
      [408, 'TIMEOUT'],
      [409, 'UNKNOWN'],
      [500, 'SERVER_ERROR'],
      [501, 'SERVER_ERROR'],
      [502, 'SERVICE_UNAVAILABLE'],
      [503, 'SERVICE_UNAVAILABLE'],
      [504, 'TIMEOUT'],
    ];
    for (const [status, code] of cases) {
      const fallback = statusFallback(status);
      expect(fallback.code).withContext(`status ${status}`).toBe(code);
      expect(fallback.message.length).withContext(`status ${status}`).toBeGreaterThan(0);
    }
  });
});

describe('knownCodeMessage', () => {
  it('knows the main backend codes and nothing else', () => {
    for (const code of [
      'INVALID_CREDENTIALS',
      'USERNAME_TAKEN',
      'EMAIL_TAKEN',
      'VALIDATION_FAILED',
      'ACCESS_DENIED',
      'UNAUTHENTICATED',
    ]) {
      expect(knownCodeMessage(code)).withContext(code).toEqual(jasmine.any(String));
    }
    expect(knownCodeMessage('SOMETHING_ELSE')).toBeUndefined();
  });
});
