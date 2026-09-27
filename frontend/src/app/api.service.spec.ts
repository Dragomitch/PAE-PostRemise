import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiService } from './api.service';
import { appConfig } from './app.config';
import { ApiProblem } from './core/api-problem';
import { SignUpRequest, User } from './core/models';

const USER: User = { id: 1, username: 'jdoe', firstName: 'John', lastName: 'Doe', email: 'j@d.be' };

describe('ApiService', () => {
  describe('with the application providers', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({ providers: [...appConfig.providers] });
    });

    it('can be injected because the app config provides HttpClient', () => {
      expect(TestBed.inject(HttpClient)).toBeTruthy();
      expect(TestBed.inject(ApiService)).toBeTruthy();
    });
  });

  describe('requests', () => {
    let service: ApiService;
    let http: HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [...appConfig.providers, provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'en' }],
      });
      service = TestBed.inject(ApiService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    // Relative URLs keep the browser on its own origin: the dev-server proxy (ng serve) or
    // nginx (Docker) forwards /api to the backend, so no cross-origin request is made.
    it('posts the credentials as JSON to the session endpoint and returns the user', () => {
      let user: User | undefined;
      service.login({ username: 'jdoe', password: 'secret' }).subscribe((u) => (user = u));

      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'jdoe', password: 'secret' });
      req.flush(USER);
      expect(user).toEqual(USER);
    });

    it('posts the user object itself (no {data} wrapper) to the users endpoint', () => {
      const body: SignUpRequest = {
        username: 'jdoe',
        password: 'secret',
        firstName: 'John',
        lastName: 'Doe',
        email: 'j@d.be',
        option: { code: 'BIN' },
      };
      service.signup(body).subscribe();

      const req = http.expectOne('/api/1.0/users');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      expect(req.request.body).not.toEqual(jasmine.objectContaining({ data: jasmine.anything() }));
      req.flush(USER, { status: 201, statusText: 'Created' });
    });

    it('reads the current user from GET /session', () => {
      let user: User | undefined;
      service.currentUser().subscribe((u) => (user = u));
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('GET');
      req.flush(USER);
      expect(user).toEqual(USER);
    });

    it('closes the session with DELETE /session', () => {
      let done = false;
      service.logout().subscribe({ complete: () => (done = true) });
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      expect(done).toBeTrue();
    });

    it('sends the UI language as Accept-Language', () => {
      service.currentUser().subscribe({ error: () => undefined });
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.headers.get('Accept-Language')).toBe('en');
      req.flush(null, { status: 401, statusText: 'Unauthorized' });
    });

    it('fails with an ApiProblem, never a raw HttpErrorResponse', () => {
      let problem: unknown;
      service.currentUser().subscribe({ error: (e) => (problem = e) });
      http
        .expectOne('/api/1.0/session')
        .flush(
          { code: 'UNAUTHENTICATED', status: 401, detail: 'Please sign in.' },
          { status: 401, statusText: 'Unauthorized', headers: { 'Content-Type': 'application/problem+json' } },
        );
      expect(problem).toBeInstanceOf(ApiProblem);
      expect((problem as ApiProblem).code).toBe('UNAUTHENTICATED');
      expect((problem as ApiProblem).detail).toBe('Please sign in.');
    });

    describe('CSRF (Spring Security SPA setup)', () => {
      const cookie = 'XSRF-TOKEN';

      afterEach(() => {
        document.cookie = `${cookie}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
      });

      it('echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN on mutating requests only', () => {
        document.cookie = `${cookie}=token-123; path=/`;

        service.login({ username: 'a', password: 'b' }).subscribe();
        const post = http.expectOne((r) => r.method === 'POST');
        expect(post.request.headers.get('X-XSRF-TOKEN')).toBe('token-123');
        post.flush(USER);

        service.currentUser().subscribe();
        const get = http.expectOne((r) => r.method === 'GET');
        expect(get.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
        get.flush(USER);
      });
    });
  });
});
