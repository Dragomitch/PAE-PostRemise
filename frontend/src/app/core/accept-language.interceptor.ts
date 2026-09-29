import { HttpInterceptorFn } from '@angular/common/http';
import { LOCALE_ID, inject } from '@angular/core';

import { toSupportedLanguage } from './locales';

/** Relative API calls only: the header is never added to third-party origins. */
function isApiRequest(url: string): boolean {
  return url.startsWith('/api/') || url.startsWith('api/');
}

/**
 * Sends the UI language (from `LOCALE_ID`, fixed at build time by @angular/localize) as
 * `Accept-Language` on every API request, so the backend localizes its Problem Details
 * (`title`, `detail`, field messages) in the language the user is reading.
 * An `Accept-Language` header set explicitly by the caller is kept.
 */
export const acceptLanguageInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiRequest(req.url) || req.headers.has('Accept-Language')) {
    return next(req);
  }
  const language = toSupportedLanguage(inject(LOCALE_ID));
  return next(req.clone({ setHeaders: { 'Accept-Language': language } }));
};
