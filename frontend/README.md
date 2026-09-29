# Frontend

Angular 20 client of the Erasmus Management Application. See [`AGENTS.md`](AGENTS.md) for the
full development guide.

## Development server

```bash
npm ci
npm start          # French UI (source locale) on http://localhost:4200/
npm run start:en   # English UI
```

API calls go to relative `/api/...` URLs; the dev server forwards them to the backend on
`http://localhost:8080` through `proxy.conf.json`, so start the backend first. In Docker Compose,
`nginx.conf` does the same towards the `backend` service. Angular's built-in XSRF support sends
the `XSRF-TOKEN` cookie back as the `X-XSRF-TOKEN` header on mutating requests (Spring Security
SPA setup); keep URLs relative for it to work.

## Internationalization

The UI uses Angular's built-in, compile-time i18n (`@angular/localize`). French (`fr`) is the
source locale, English (`en`) is a translation (`src/locale/messages.en.xlf`). A missing
translation fails the build (`i18nMissingTranslation: "error"`).

To add or change a string:

1. Mark it with a stable id: `<p i18n="@@feature.element">Texte en français</p>`,
   `i18n-<attribute>="@@..."` for attributes, or `` $localize`:@@feature.element:Texte` `` in TypeScript.
2. `npm run i18n:extract` updates `src/locale/messages.xlf`.
3. Add the English `<target>` for the id in `src/locale/messages.en.xlf`.
4. `npm run i18n:check` verifies that the catalog is up to date and every translation is
   present and current (it also runs in `npm run test:ci`).

## Error handling

Every backend error is an RFC 9457 `application/problem+json` body (`type`, `title`, `status`,
`detail`, `instance`, a stable `code` such as `USERNAME_TAKEN`, and `errors[]` for validation),
localized from the `Accept-Language` header that the app sets from its locale.
`ApiService` turns every failure into an `ApiProblem` (`src/app/core/api-problem.ts`): the
backend's localized `detail` is shown when present; otherwise the app shows its own localized
message (known codes, then network/timeout/server categories). Raw responses are never displayed.

## Building

```bash
npm run build
```

The production build is localized: it writes `dist/frontend/browser/fr/` and
`dist/frontend/browser/en/`, each with its own `<base href>`. Nginx redirects `/` to the
preferred language (`Accept-Language`, default French).

## Running unit tests

```bash
npm test          # interactive Karma
npm run test:ci   # i18n check + headless run, JUnit and coverage reports, coverage thresholds
```

Headless Chrome may need `CHROME_BIN` pointing to a Chrome/Chromium binary.
