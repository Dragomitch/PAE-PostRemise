import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiService } from './api.service';
import { appConfig } from './app.config';

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
        providers: [...appConfig.providers, provideHttpClientTesting()],
      });
      service = TestBed.inject(ApiService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    // Relative URLs keep the browser on its own origin: the dev-server proxy (ng serve) or
    // nginx (Docker) forwards /api to the backend, so no cross-origin request is made.
    it('posts the credentials to the relative session endpoint', () => {
      service.login({ username: 'jdoe', password: 'secret' }).subscribe();

      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'jdoe', password: 'secret' });
      req.flush({});
    });

    it('posts the sign-up data to the relative users endpoint', () => {
      service.signup({ username: 'jdoe' }).subscribe();

      const req = http.expectOne('/api/1.0/users');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ data: JSON.stringify({ username: 'jdoe' }) });
      req.flush({});
    });
  });
});
