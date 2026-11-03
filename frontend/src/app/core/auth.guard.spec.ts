import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, GuardResult, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, isObservable, of } from 'rxjs';

import { authGuard, roleGuard } from './auth.guard';
import { SessionService } from './session.service';

describe('authGuard', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function runGuard(): Promise<GuardResult> {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/partners' } as RouterStateSnapshot),
    );
    const observable: Observable<GuardResult> = isObservable(result) ? result : of(result as GuardResult);
    return firstValueFrom(observable);
  }

  it('lets a known user through without asking the server', async () => {
    const session = TestBed.inject(SessionService);
    session.signIn({ username: 'alice', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush({ username: 'alice', role: 'Student' });

    expect(await runGuard()).toBeTrue();
    http.expectNone('/api/1.0/session');
  });

  it('keeps the session after a page reload by asking GET /session', async () => {
    const result = runGuard();
    const req = http.expectOne('/api/1.0/session');
    expect(req.request.method).toBe('GET');
    req.flush({ username: 'alice', role: 'Student' });

    expect(await result).toBeTrue();
    expect(TestBed.inject(SessionService).user()?.username).toBe('alice');
  });

  it('sends an anonymous visitor to /signin', async () => {
    const result = runGuard();
    http.expectOne('/api/1.0/session').flush(
      { code: 'UNAUTHENTICATED', status: 401 },
      { status: 401, statusText: 'Unauthorized', headers: { 'Content-Type': 'application/problem+json' } },
    );

    const outcome = await result;
    expect(outcome instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(outcome as UrlTree)).toBe('/signin');
  });

  it('sends the visitor to /signin when the backend is unreachable', async () => {
    const result = runGuard();
    http.expectOne('/api/1.0/session').error(new ProgressEvent('error'));
    expect(router.serializeUrl((await result) as UrlTree)).toBe('/signin');
  });
});

describe('roleGuard', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function signIn(role: string): void {
    TestBed.inject(SessionService).signIn({ username: 'someone', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush({ username: 'someone', role });
  }

  function runGuard(role: string): GuardResult {
    return TestBed.runInInjectionContext(() =>
      roleGuard(role)({} as ActivatedRouteSnapshot, { url: '/users' } as RouterStateSnapshot),
    ) as GuardResult;
  }

  it('lets a user having the role through', () => {
    signIn('Professor');
    expect(runGuard('Professor')).toBeTrue();
  });

  it('sends a user of another role to the partners, without asking the server', () => {
    signIn('Student');
    const outcome = runGuard('Professor');
    expect(outcome instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(outcome as UrlTree)).toBe('/partners');
  });

  it('sends an unknown user to the partners (the shell guard runs first and signs them in)', () => {
    expect(router.serializeUrl(runGuard('Student') as UrlTree)).toBe('/partners');
  });
});
