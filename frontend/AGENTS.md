---
name: "Erasmus-Management-Application Frontend"
description: "Guidelines for the Angular client."
category: "Frontend Development"
author: "Dragomitch"
authorUrl: "https://github.com/Dragomitch"
tags: ["Angular", "TypeScript", "Node.js"]
lastUpdated: "2026-09-27"
---

# EMA Frontend Guide

## Project Overview

This document covers the development workflow for the Angular frontend of the Erasmus Management Application. The frontend consumes the REST API exposed by the backend and provides the user interface.

## Tech Stack

- **Framework**: Angular 20 (standalone components, signals, reactive forms)
- **Language**: TypeScript
- **i18n**: `@angular/localize` (compile-time, one build per locale: `fr` source, `en`)
- **Package Manager**: npm
- **Styling**: CSS
- **Testing**: Karma & Jasmine (random order, coverage thresholds)

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/                  # cross-cutting code (no UI)
│   │   │   ├── problem-detail.ts          # RFC 9457 model + type guard
│   │   │   ├── api-problem.ts             # HttpErrorResponse -> ApiProblem (+ localized fallbacks)
│   │   │   ├── accept-language.interceptor.ts
│   │   │   ├── form-errors.ts             # client/server field error helpers
│   │   │   ├── locales.ts                 # supported locales (fr, en)
│   │   │   └── models.ts                  # API DTOs
│   │   ├── language-switcher/
│   │   ├── sign-in/  sign-up/
│   │   └── api.service.ts
│   └── locale/
│       ├── messages.xlf           # extracted French source catalog (generated, committed)
│       └── messages.en.xlf        # English translations (hand-maintained)
├── scripts/check-i18n.mjs         # CI guard: stale catalog / missing or outdated translations
├── angular.json  karma.conf.js  proxy.conf.json  nginx.conf  Dockerfile
└── package.json
```

## Development Guidelines

### Code Style

- Use the Angular CLI formatting tools (`ng lint`).
- Prefer single quotes and end files with a newline.
- Keep components small and focused.
- Never hard-code user-facing text without an i18n marker (see below).

### Naming Conventions

- File naming: kebab-case (`my-component.ts`).
- Variable naming: camelCase.
- Function naming: camelCase.
- Class naming: PascalCase.
- i18n ids: `@@<feature>.<element>[.<detail>]` (e.g. `@@signin.username.label`).

### Git Workflow

- Use `feature/` or `bugfix/` branches off `master`.
- Commit messages follow `<type>: <short description>`.
- Submit Pull Requests with screenshots for UI changes when possible.

## Environment Setup

### Development Requirements

- Node.js 20+
- npm 10+
- Angular CLI 20

### Installation Steps

```bash
cd frontend
npm ci
npm start        # French UI on http://localhost:4200 (backend expected on :8080)
npm run start:en # English UI (dev server builds one locale at a time)
```

The dev server serves a single locale at `/`, so the language switcher's `/en/...` link only
works against the production build (nginx / Docker Compose).

### API calls, CORS and CSRF

`ApiService` uses relative URLs (`/api/1.0/...`) so the browser always talks to the origin that
served the app:

- `ng serve`: `proxy.conf.json` (wired in `angular.json` under `serve.options.proxyConfig`)
  forwards `/api` to `http://localhost:8080` for both `start` and `start:en`.
- Docker Compose: `nginx.conf` serves the build and proxies `/api` to `http://backend:8080`.

Do not hard-code `http://localhost:8080` in services. If the app must call the backend directly
from another origin, add that origin to the backend's `app.cors.allowed-origins` property
(env var `APP_CORS_ALLOWED_ORIGINS`, default `http://localhost:4200`).

CSRF: Spring Security uses the standard SPA setup (cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`).
Angular's `HttpClient` XSRF support handles it: `app.config.ts` declares
`withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' })` (the defaults,
spelled out on purpose). Angular only adds the header to **relative, mutating** requests
(POST/PUT/PATCH/DELETE), which is one more reason to keep API URLs relative. Never add
`withNoXsrfProtection()`.
The app is served by nginx, so a fresh browser has no `XSRF-TOKEN` cookie until an API response
sets it: `core/csrf-bootstrap.ts` (an app initializer) makes one GET to the public
`/api/1.0/options` when the cookie is missing, otherwise the first sign-in/sign-up POST would be
rejected with 403. It never blocks start-up.

## Core Feature Implementation

### Internationalization (@angular/localize)

Why compile-time `@angular/localize` rather than a runtime library (ngx-translate, Transloco):
it is Angular's built-in, officially recommended solution; translations are inlined at build
time so there is no translation file to download, no flash of untranslated keys and no runtime
lookup that can miss; and `i18nMissingTranslation: "error"` turns a missing translation into a
**build failure** instead of a key shown to users. Trade-off: one build (and URL prefix) per
locale, and switching language reloads the page.

Configuration (`angular.json`):

- `i18n.sourceLocale` = `fr` (subPath `fr`), `i18n.locales.en` = `src/locale/messages.en.xlf`
  (subPath `en`). Each locale gets `<base href="/<subPath>/">` and `<html lang>` automatically.
- `build.options.i18nMissingTranslation: "error"`; `production` sets `localize: true`, so
  `npm run build` writes `dist/frontend/browser/fr/` and `dist/frontend/browser/en/`.
- Build configurations `fr` / `en` (single locale) are used by the dev server configurations
  `development` (default, fr) and `development-en`.

Adding or changing a string:

1. Mark it with a stable custom id:
   - template text: `<h1 i18n="@@signin.title">Bienvenue à bord</h1>`
   - attributes: `<nav aria-label="Choix de la langue" i18n-aria-label="@@languageSwitcher.ariaLabel">`
   - interpolations get a named placeholder: `{{ name // i18n(ph="NAME") }}`
   - TypeScript: `` $localize`:@@problem.fallback.network:Impossible de joindre le serveur...` ``
   Write the French text (source locale) in the code. Keep ids stable: the id, not the text,
   links a message to its translations.
2. `npm run i18n:extract` regenerates `src/locale/messages.xlf` (commit it).
3. Add/update the `<trans-unit>` in `src/locale/messages.en.xlf`: copy the `<source>` from
   `messages.xlf` and add `<target>` with the English text (keep `<x id="..."/>` placeholders).
4. `npm run i18n:check` must pass (it also runs as the first step of `npm run test:ci`).

What protects the catalogs:

| Problem | Caught by |
| --- | --- |
| Id missing from `messages.en.xlf` | `ng build` (`i18nMissingTranslation: "error"`) and `i18n:check` |
| `<trans-unit>` present but `<target>` missing (Angular silently uses the French source) | `i18n:check` |
| French text changed but English not updated | `i18n:check` (compares the `<source>` copies) |
| `messages.xlf` not re-extracted | `i18n:check` |
| Obsolete translations | `i18n:check` |

Endonyms in the language switcher ("Français", "English") are intentionally not translated.

### Error handling (RFC 9457 Problem Details)

Contract with the backend: every error response is `application/problem+json`:

```json
{
  "type": "urn:pae:problem:username-taken",
  "title": "Conflit",
  "status": 409,
  "detail": "Ce nom d'utilisateur est déjà utilisé.",
  "instance": "/api/1.0/users",
  "code": "USERNAME_TAKEN",
  "errors": [{ "field": "email", "code": "Email", "message": "Adresse invalide" }]
}
```

`title`, `detail` and `errors[].message` are localized by the backend from `Accept-Language`;
`code` is a stable UPPER_SNAKE key; `errors` only appears for validation failures.

Frontend pieces:

- `acceptLanguageInterceptor` sets `Accept-Language` (`fr` or `en`, from `LOCALE_ID`) on every
  relative `/api` request, unless the caller set one.
- `ApiService` methods pipe `mapApiProblem()`, so they **always fail with an `ApiProblem`**
  (`status`, `code`, `title`, `detail`, `errors`, `fieldErrors`, `fromServer`), never with a raw
  `HttpErrorResponse`.
- `toApiProblem()` uses the backend problem when the body is a valid Problem Details object
  (`isProblemDetail`). `detail` priority: backend `detail` > frontend message for a known code
  (`INVALID_CREDENTIALS`, `USERNAME_TAKEN`, `EMAIL_TAKEN`, `VALIDATION_FAILED`, `ACCESS_DENIED`,
  `UNAUTHENTICATED`) > backend `title` > status fallback. Anything else (status 0, proxy HTML
  502/504, non-JSON body, timeout, exception) gets a localized fallback with a frontend code:
  `NETWORK_ERROR`, `TIMEOUT`, `SERVICE_UNAVAILABLE`, `SERVER_ERROR`, `UNAUTHENTICATED`,
  `ACCESS_DENIED`, `NOT_FOUND` or `UNKNOWN`. Raw bodies, HTML and stack traces are never shown.
- Components show `problem.detail` in a `role="alert"` block, field errors next to the inputs
  (`problem.fieldError('email')`, wired with `aria-invalid` / `aria-describedby`), list field
  errors that match no input inside the alert, and disable the submit button while a request is
  in flight. Branch on `problem.code`, never on message text.

### API endpoints used

- `POST /api/1.0/session` `{username, password}` -> user + HttpOnly session cookie
- `GET /api/1.0/session` -> current user or 401; `DELETE /api/1.0/session` -> sign out
- `POST /api/1.0/users` with the user object as body:
  `{username, password, firstName, lastName, email, option: {code}}`

The sign-up option list is static (`BIN`, `BBM`, `BCH`, `BDI`, `BIM`, from `SQLRessources/init.sql`)
until the backend exposes an options endpoint in the new API.

### Routing

Routing is defined in `app.routes.ts`; route titles are localized with `$localize`.
Unknown paths redirect to `/signin`.

## Testing Strategy

### Unit Testing

- Jasmine with Karma, **random order** (`client.jasmine.random`): specs must not share state.
- HTTP code is tested with `provideHttpClientTesting()` / `HttpTestingController`
  (`afterEach(() => http.verify())`).
- `npm run test:ci` = `i18n:check` + headless run with JUnit (`reports/junit`) and coverage
  (`coverage/frontend`). `karma.conf.js` enforces global coverage thresholds
  (statements/functions/lines 97 %, branches 95 %); coverage is currently 100 %. Raise the
  thresholds with coverage, never lower them to get a green build.
- In tests `$localize` returns the French source text (no translation is loaded).
- Headless Chrome needs `CHROME_BIN` when Chrome is not installed system-wide.

### End-to-End Testing

Not set up yet.

## Deployment Guide

### Build Process

```bash
npm run build   # production, localize: true -> dist/frontend/browser/{fr,en}/
```

### Deployment Steps

1. The Docker image copies `dist/frontend/browser` (both locales) into Nginx.
2. `nginx.conf`: `/` (and any unprefixed path) redirects with 302 + `Vary: Accept-Language` to
   `/fr/...` or `/en/...` (first of fr/en in `Accept-Language`, default fr); `/fr/` and `/en/`
   each have their own SPA fallback to their `index.html`; missing static files return 404;
   `/api/` is proxied to `http://backend:8080` (headers such as `Accept-Language` and the
   XSRF cookie/header pass through).
3. Verify the application loads on port 4200.

## Performance Optimization

- Translations are inlined at build time: no runtime i18n cost.
- Use `ChangeDetectionStrategy.OnPush` where appropriate (e.g. `LanguageSwitcher`).

## Security Considerations

- Validate user input on the client when possible; the server's validation is authoritative.
- Angular escapes interpolations; never bind server messages with `[innerHTML]`.
- The session is an HttpOnly cookie managed by the backend; nothing is stored in web storage.
- Keep XSRF protection enabled (see above).

## Monitoring and Logging

- Use `console.log` sparingly and remove debug statements in production builds.

## Common Issues

### Issue 1: Dependency errors after a pull

**Solution**: Delete `node_modules` and run `npm ci` again.

### Issue 2: API requests fail in development

**Solution**: Check that the backend is running on port 8080 and that you started the app with
`npm start` / `npm run start:en` (which apply `proxy.conf.json`). A CORS error means the app is
calling the backend on another origin instead of the relative `/api` path. With the backend down,
the dev proxy answers 500 and the UI shows the localized "server error" message.

### Issue 3: `No translation found for "<id>"` during `ng build`

**Solution**: Add the message to `src/locale/messages.en.xlf` (see the i18n workflow), then run
`npm run i18n:check`.

### Issue 4: `i18n:check failed`

**Solution**: Follow the listed actions: run `npm run i18n:extract`, then update
`messages.en.xlf` for every new, changed or removed id.

## Reference Resources

- [Angular i18n guide](https://angular.dev/guide/i18n)
- [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [Angular HttpClient security (XSRF)](https://angular.dev/best-practices/security#httpclient-xsrf-csrf-security)
- [Angular Documentation](https://angular.dev/docs)
