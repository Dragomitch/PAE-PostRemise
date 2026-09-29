import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { acceptLanguageInterceptor } from './accept-language.interceptor';

describe('acceptLanguageInterceptor', () => {
  function setup(localeId: string): { http: HttpClient; controller: HttpTestingController } {
    TestBed.configureTestingModule({
      providers: [
        { provide: LOCALE_ID, useValue: localeId },
        provideHttpClient(withInterceptors([acceptLanguageInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    return { http: TestBed.inject(HttpClient), controller: TestBed.inject(HttpTestingController) };
  }

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('sends the French locale on API requests', () => {
    const { http, controller } = setup('fr');
    http.get('/api/1.0/session').subscribe();
    const req = controller.expectOne('/api/1.0/session');
    expect(req.request.headers.get('Accept-Language')).toBe('fr');
    req.flush({});
  });

  it('sends the English locale on every method, including relative "api/" URLs', () => {
    const { http, controller } = setup('en');
    http.post('/api/1.0/users', {}).subscribe();
    http.delete('api/1.0/session').subscribe();
    expect(controller.expectOne('/api/1.0/users').request.headers.get('Accept-Language')).toBe('en');
    expect(controller.expectOne('api/1.0/session').request.headers.get('Accept-Language')).toBe('en');
  });

  it('reduces regional locales to a supported language', () => {
    const { http, controller } = setup('en-US');
    http.get('/api/1.0/session').subscribe();
    expect(controller.expectOne('/api/1.0/session').request.headers.get('Accept-Language')).toBe('en');
  });

  it('falls back to French for unsupported locales', () => {
    const { http, controller } = setup('de-DE');
    http.get('/api/1.0/session').subscribe();
    expect(controller.expectOne('/api/1.0/session').request.headers.get('Accept-Language')).toBe('fr');
  });

  it('does not touch non-API or cross-origin requests', () => {
    const { http, controller } = setup('en');
    http.get('/assets/i18n.json').subscribe();
    http.get('https://example.org/api/1.0/x').subscribe();
    expect(controller.expectOne('/assets/i18n.json').request.headers.has('Accept-Language')).toBeFalse();
    expect(controller.expectOne('https://example.org/api/1.0/x').request.headers.has('Accept-Language')).toBeFalse();
  });

  it('keeps an explicit Accept-Language header', () => {
    const { http, controller } = setup('fr');
    http.get('/api/1.0/session', { headers: { 'Accept-Language': 'en' } }).subscribe();
    expect(controller.expectOne('/api/1.0/session').request.headers.get('Accept-Language')).toBe('en');
  });
});
