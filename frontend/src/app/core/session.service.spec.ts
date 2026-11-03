import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiProblem } from './api-problem';
import { User } from './models';
import { SessionService } from './session.service';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const PROFESSOR: User = { id: 1, username: 'prof', firstName: 'Paula', lastName: 'Prof', role: 'Professor' };
const STUDENT: User = { id: 2, username: 'alice', firstName: 'Alice', lastName: 'Student', role: 'Student' };

describe('SessionService', () => {
  let session: SessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    session = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function unauthenticated(): void {
    http.expectOne('/api/1.0/session').flush(
      { code: 'UNAUTHENTICATED', status: 401, detail: 'Please sign in.' },
      { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
    );
  }

  it('starts anonymous', () => {
    expect(session.user()).toBeNull();
    expect(session.isProfessor()).toBeFalse();
  });

  describe('signIn', () => {
    it('posts the credentials and remembers the user', () => {
      let signedIn: User | undefined;
      session.signIn({ username: 'prof', password: 'secret' }).subscribe((u) => (signedIn = u));
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'prof', password: 'secret' });
      expect(session.user()).toBeNull();
      req.flush(PROFESSOR);

      expect(signedIn).toEqual(PROFESSOR);
      expect(session.user()).toEqual(PROFESSOR);
      expect(session.isProfessor()).toBeTrue();
    });

    it('knows a student is not a professor', () => {
      session.signIn({ username: 'alice', password: 'secret' }).subscribe();
      http.expectOne('/api/1.0/session').flush(STUDENT);
      expect(session.isProfessor()).toBeFalse();
    });

    it('fails with an ApiProblem and stays anonymous when the credentials are refused', () => {
      let problem: unknown;
      session.signIn({ username: 'prof', password: 'bad' }).subscribe({ error: (e) => (problem = e) });
      unauthenticated();
      expect(problem).toBeInstanceOf(ApiProblem);
      expect(session.user()).toBeNull();
    });
  });

  describe('restore', () => {
    it('reads the current user from GET /session', () => {
      let restored: User | null | undefined;
      session.restore().subscribe((u) => (restored = u));
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('GET');
      req.flush(STUDENT);

      expect(restored).toEqual(STUDENT);
      expect(session.user()).toEqual(STUDENT);
    });

    it('answers null without failing when there is no valid session', () => {
      session.signIn({ username: 'prof', password: 'secret' }).subscribe();
      http.expectOne('/api/1.0/session').flush(PROFESSOR);

      let restored: User | null | undefined;
      let failed = false;
      session.restore().subscribe({ next: (u) => (restored = u), error: () => (failed = true) });
      unauthenticated();

      expect(failed).toBeFalse();
      expect(restored).toBeNull();
      expect(session.user()).toBeNull();
    });

    it('answers null when the backend cannot be reached', () => {
      let restored: User | null | undefined;
      session.restore().subscribe((u) => (restored = u));
      http.expectOne('/api/1.0/session').error(new ProgressEvent('error'));
      expect(restored).toBeNull();
    });
  });

  describe('signOut', () => {
    beforeEach(() => {
      session.signIn({ username: 'prof', password: 'secret' }).subscribe();
      http.expectOne('/api/1.0/session').flush(PROFESSOR);
    });

    it('calls DELETE /session, then forgets the user', () => {
      let done = false;
      session.signOut().subscribe({ complete: () => (done = true) });
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('DELETE');
      expect(session.user()).toEqual(PROFESSOR);
      req.flush(null);

      expect(done).toBeTrue();
      expect(session.user()).toBeNull();
    });

    it('treats a session the server no longer knows (401) as closed', () => {
      let failed = false;
      session.signOut().subscribe({ error: () => (failed = true) });
      unauthenticated();
      expect(failed).toBeFalse();
      expect(session.user()).toBeNull();
    });

    it('keeps the user and reports the problem when the server could not close the session', () => {
      let problem: unknown;
      session.signOut().subscribe({ error: (e) => (problem = e) });
      http.expectOne('/api/1.0/session').flush(null, { status: 500, statusText: 'Server Error' });

      expect(problem).toBeInstanceOf(ApiProblem);
      expect((problem as ApiProblem).code).toBe('SERVER_ERROR');
      expect(session.user()).toEqual(PROFESSOR);
    });
  });

  it('clear forgets the user without calling the server', () => {
    session.signIn({ username: 'prof', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(PROFESSOR);
    session.clear();
    expect(session.user()).toBeNull();
  });
});
