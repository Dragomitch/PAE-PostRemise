import {
  ApplicationConfig,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { acceptLanguageInterceptor } from './core/accept-language.interceptor';
import { initCsrfToken } from './core/csrf-bootstrap';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      withInterceptors([acceptLanguageInterceptor]),
      // Spring Security's SPA CSRF setup: cookie XSRF-TOKEN -> header X-XSRF-TOKEN (Angular's
      // defaults, spelled out so nobody disables it by accident). Only relative, mutating
      // requests carry the header.
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    // Obtain the XSRF-TOKEN cookie before the first form can be submitted.
    provideAppInitializer(() => initCsrfToken()),
  ],
};
