import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { hasXsrfCookie, initCsrfToken } from './csrf-bootstrap';

describe('csrf bootstrap', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('detects the XSRF-TOKEN cookie among others', () => {
    expect(hasXsrfCookie('a=1; XSRF-TOKEN=abc; b=2')).toBeTrue();
    expect(hasXsrfCookie('XSRF-TOKEN=abc')).toBeTrue();
    expect(hasXsrfCookie('')).toBeFalse();
    expect(hasXsrfCookie('NOT-XSRF-TOKEN=abc; session=x')).toBeFalse();
  });

  it('fetches a public endpoint when the cookie is missing', async () => {
    const done = TestBed.runInInjectionContext(() => initCsrfToken({ cookie: 'session=x' }));
    http.expectOne({ method: 'GET', url: '/api/1.0/options' }).flush([]);
    await expectAsync(done).toBeResolved();
  });

  it('does not call the API when the cookie is already there', async () => {
    const done = TestBed.runInInjectionContext(() => initCsrfToken({ cookie: 'XSRF-TOKEN=abc' }));
    http.expectNone('/api/1.0/options');
    await expectAsync(done).toBeResolved();
  });

  it('never blocks start-up when the backend is unreachable', async () => {
    const done = TestBed.runInInjectionContext(() => initCsrfToken({ cookie: '' }));
    http.expectOne('/api/1.0/options').error(new ProgressEvent('error'), { status: 0 });
    await expectAsync(done).toBeResolved();
  });
});
