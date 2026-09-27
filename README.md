# Erasmus Mobility Management

[![CI](https://github.com/Dragomitch/PAE-PostRemise/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Dragomitch/PAE-PostRemise/actions/workflows/ci.yml)

This repository contains a web application for managing Erasmus mobilities. It was created as a second-year project at the Institut Paul Lambin during the 2015–2016 academic year.

## General structure
- **src/main/java**
  - `business` – entity interfaces and implementations (`User`, `Mobility`, …), the DTO interfaces carrying the Bean Validation constraints, the custom constraints (`business/validation`) and the error catalogue (`business/exceptions/ErrorCode`).
  - `persistence` – DAO interfaces and their JDBC implementations.
  - `uccontrollers` – use cases (`@Service`, `@Transactional`) that orchestrate the business logic and the DAOs.
  - `web` – the REST API: one Spring MVC `@RestController` per resource under `/api/1.0`, and `ApiExceptionHandler` which renders every API error.
  - `security` – session JWT cookie, current user, CSRF helpers used by the security chain.
  - `config` – Spring configuration (security chain, JSON mapping, CORS, web UI routing).
  - additional packages include `exceptions` and `utils`; `Application` is the Spring Boot entry point.
- **src/main/resources** contains `application.properties` (Spring configuration) and the message bundles `i18n/messages*.properties` (error titles and details, validation messages, in French and English).
- **src/main/webapp** hosts the legacy client-side HTML, CSS and JavaScript, packaged as static content and served at `/`.
- **src/test/java** holds the JUnit 5 tests. Unit tests run the real business and use-case beans against the in-memory mock DAOs (`persistence/mocks`, wired by `UnitTestConfig`); the `web` tests are `@WebMvcTest` slices with the real security chain and mocked use cases; `ApplicationTests` boots the whole application without a database; the `*IT` DAO tests run against an embedded PostgreSQL.

## Key design aspects
- **Layers**: controllers (`web`) → use cases (`uccontrollers`) → DAOs (`persistence`), all Spring beans wired by constructor injection. Business objects are created through `EntityFactory`, which binds each business/DTO interface to its implementation; the same bindings tell Spring MVC's Jackson `ObjectMapper` how to read the DTO interfaces (`JacksonConfig`).
- **REST API**: thin `@RestController`s bind the request (JSON body, path and query parameters, `CurrentUser`), call a use case and return its result. Paths are matched case-insensitively (as the former router did). Lists read by DataTables are wrapped in `{"data": [...]}`; exports are `text/csv`.
- **Transactions**: every use-case service is `@Transactional` (read-only for queries); the DAOs prepare their statements on the connection of the current Spring transaction (`DataSourceUtils`) and refuse to run outside one. Updates check the entity version (`WHERE ... AND version = ?`) and throw a `ConcurrentModificationException` (409 `CONCURRENT_MODIFICATION` problem) on a stale version.
- **Security** (Spring Security, stateless, see `SecurityConfig`):
  - `POST /api/1.0/session` with `{"username", "password"}` issues an HS256 JWT (`sub` = user id, `role`, `iat`, `exp`) in the `session` cookie: `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure` when `APP_SESSION_COOKIE_SECURE=true`, valid `APP_SESSION_VALIDITY` (12h). `GET` returns the current user, `DELETE` expires the cookie. No HTTP session is created.
  - The OAuth2 resource server validates the token from the cookie (or an `Authorization: Bearer` header); the `role` claim becomes `ROLE_PROFESSOR` / `ROLE_STUDENT`, checked by `@PreAuthorize` on the controllers. Every `/api/**` endpoint requires a session except sign-in, sign-up (`POST /users`) and the option list (`GET /options`).
  - CSRF: the `XSRF-TOKEN` cookie (readable by JavaScript) is issued on every response and must be echoed in the `X-XSRF-TOKEN` header of every `POST`/`PUT`/`DELETE`, sign-in included (Angular does it natively, `app.js` in `$.ajaxSetup`). Requests authenticated by an `Authorization` header are exempt.
  - Errors (401, 403 included) are RFC 9457 problem details, see [Error contract](#error-contract-rfc-9457-problem-details).
- **Entry point**: `Application` (Spring Boot, embedded Tomcat).
- **Validation**: Bean Validation constraints on the DTOs, the request parameters and the use-case methods, see [Validation conventions](#validation-conventions).
- **i18n**: French (default) and English messages, chosen from `Accept-Language`, see [Translations](#translations-i18n).

## Error contract (RFC 9457 problem details)
Every API error, whatever its origin (business rule, validation, Spring MVC, Spring Security, database, unexpected exception), is answered by `ApiExceptionHandler` with `Content-Type: application/problem+json`:

```json
{
  "type": "urn:pae:problem:validation-failed",
  "title": "Données invalides",
  "status": 400,
  "detail": "Certaines informations sont invalides. Corrigez les champs indiqués.",
  "instance": "/api/1.0/users",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "field": "email", "code": "Email", "message": "doit être une adresse e-mail valide" },
    { "field": "username", "code": "Size", "message": "doit contenir au plus 20 caractères" }
  ],
  "timestamp": "2026-09-27T21:41:52.039Z"
}
```

| Member | Content |
|--------|---------|
| `type` | `urn:pae:problem:<kebab-case code>` |
| `title`, `detail` | Localized from `Accept-Language` (French by default, English); the detail may name the value (`Le nom d’utilisateur « stud » est déjà utilisé.`) |
| `status` | The HTTP status |
| `instance` | The path of the request |
| `code` | Always present: the stable machine key (`ErrorCode`) clients branch on, never the text |
| `errors` | Only for `VALIDATION_FAILED`: `field` (path in the body or parameter name, e.g. `address.city`, `options[0].code`, `id`), `code` (constraint name), `message` (localized) |
| `timestamp` | When the error occurred |
| `errorId` | Only for 5xx: the reference written in the error log with the stack trace. A 5xx never describes the failure (no SQL, no stack trace) |

Status policy: 400 validation / malformed request, 401/403 security, 404 unknown resource of the URL, 409 conflict with the current state (uniqueness, state of a mobility or choice, stale version), 422 business rule broken by a well-formed request (unknown referenced entity...), 5xx server failures.

| Code | Status | Meaning | Former numeric codes |
|------|--------|---------|----------------------|
| `VALIDATION_FAILED` | 400 | Constraints broken, listed in `errors` | 110, 130 and every field code (132-140, 201-216, 302-316, 401-402, 601-619, 701-708, 800-810, 323, 508, 709) |
| `MALFORMED_REQUEST` | 400 | Unreadable JSON, missing or wrongly typed parameter | - |
| `UNAUTHENTICATED` | 401 | No, invalid or expired session | 101 |
| `INVALID_CREDENTIALS` | 401 | Wrong username or password at sign-in | 101 |
| `ACCESS_DENIED` | 403 | Role, ownership or CSRF check failed | 103 |
| `RESOURCE_NOT_FOUND` | 404 | Unknown resource of the URL, unknown route | 104 |
| `METHOD_NOT_ALLOWED` / `NOT_ACCEPTABLE` / `UNSUPPORTED_MEDIA_TYPE` / `PAYLOAD_TOO_LARGE` | 405 / 406 / 415 / 413 | HTTP-level errors | - |
| `CONCURRENT_MODIFICATION` | 409 | Stale version (optimistic locking) | 120 (was a 400) |
| `DATA_CONFLICT` | 409 | Database constraint refused the change | - |
| `USERNAME_TAKEN` / `EMAIL_TAKEN` | 409 | Uniqueness of the account | 204 / 207 |
| `MOBILITY_CHOICE_ALREADY_CONFIRMED` | 409 | Choice already turned into a mobility | 301, 321 |
| `MOBILITY_CHOICE_CLOSED` | 409 | Choice already cancelled or rejected | 317 |
| `PARTNER_REQUIRED_TO_CONFIRM` | 409 | Confirmation without partner | 324 |
| `MOBILITY_CANCELLED` / `MOBILITY_CLOSED` | 409 | State of the mobility | 501 / 502 |
| `DEPARTURE_DOCUMENTS_INCOMPLETE` / `RETURN_DOCUMENTS_INCOMPLETE` / `DOCUMENTS_INCOMPLETE` | 409 | Documents not filled in | 503 / 504 / 507 |
| `INCOMPLETE_BANK_DETAILS` | 409 | Payment without IBAN/BIC/bank | 506 |
| `PAYMENT_NOT_EXPECTED` | 409 | No payment in this state | 110 (confirmPayment) |
| `ALREADY_NOMINATED` | 409 | Personal data already recorded | 618 |
| `PARTNER_HAS_MOBILITY_CHOICES` / `PARTNER_NOT_ARCHIVED` | 409 | Archive / restore a partner | 710 / 711 |
| `UNKNOWN_USER` / `UNKNOWN_OPTION` / `UNKNOWN_COUNTRY` / `UNKNOWN_PROGRAMME` / `UNKNOWN_DENIAL_REASON` / `UNKNOWN_DOCUMENT` | 422 | The body references an entity that does not exist | 200 / 210 / 900 / 1000 / 134, 400 / 505 |
| `PROFESSOR_CANNOT_APPLY` | 422 | A professor applies for himself | 322 |
| `COUNTRY_CHANGE_NOT_ALLOWED` | 422 | Partner outside the country of the choice | 320 |
| `PARTNER_OPTION_REQUIRED` | 422 | A partner keeps at least one option | 712 |
| `DENIAL_REASON_REQUIRED` / `CANCELLATION_REASON_REQUIRED` | 422 | Reason missing to cancel a mobility (professor / student) | 130 |
| `INTERNAL_ERROR` / `SERVICE_UNAVAILABLE` | 500 / 503 | Server failure, with an `errorId` | 100 |

### Adding an error code
1. Add a constant to `ErrorCode` with its status (see the policy above).
2. Add `problem.<CODE>.title` and `problem.<CODE>.detail` to `src/main/resources/i18n/messages.properties` (French) **and** `messages_en.properties`. The detail may use the arguments of the exception (`{0}`, `{1}`); use the typographic apostrophe (`’`), a plain `'` is a quote in these MessageFormat patterns.
3. Throw `new BusinessException(ErrorCode.MY_CODE, arg0, ...)` from the use case (`ResourceNotFoundException`, `InsufficientPermissionException` and `InvalidCredentialsException` exist for the common cases).
4. `MessageBundlesTest` fails the build if a translation is missing.

### Translations (i18n)
- Bundles: `i18n/messages.properties` (French, product language and fallback: an unsupported language gets French, never the server locale) and `i18n/messages_en.properties`, UTF-8 (`spring.messages.*` in `application.properties`).
- They hold the problem texts (`problem.<CODE>.*`), the texts of the Spring MVC errors (Spring's own message codes `problemDetail.title.<exception class>` / `problemDetail.<exception class>`) and the Bean Validation messages (standard `jakarta.validation.constraints.*.message` keys and the application's `pae.validation.*` keys): Spring Boot's validator resolves the `{key}` templates from the same `MessageSource`.
- The language is negotiated from `Accept-Language` among `fr` and `en` (`AcceptLanguageLocaleResolver`), for the 401/403 of the security filters too. Both web UIs send the header.
- To add a language: add `messages_<lang>.properties` with every key, add the locale to `WebConfig.SUPPORTED_LOCALES` and to `MessageBundlesTest`.

## Validation conventions
- Rules on the data are Bean Validation constraints declared on the **getters of the DTO interfaces** (`business/dto`), e.g. `@NotBlank @Size(max = USERNAME_MAX_LENGTH) String getUsername()`; the lengths are those of the database columns. Custom constraints live in `business/validation`: `@Iban` (format and MOD 97 check digits), `@Bic`, `@PhoneNumber` and the class-level `@FilterValueRequired` (cross-field rule of `PartnerSearch`). Empty values are only reported by `@NotBlank`.
- Groups (`ValidationGroups`): `Default` for an entity sent in full; `OnCreate` for the extra rules of a creation (password at sign-up, at least one partner option), validated with `@Validated({Default.class, OnCreate.class})`; `Reference` for an entity referenced by another one (`option` of a user, `country` of an address, `user` and `programme` of a mobility choice): the referencing getter uses `@Valid @ConvertGroup(from = Default.class, to = Reference.class)` and only the identifier is checked.
- Web layer: `@Valid` (or `@Validated(groups)`) on every `@RequestBody`, constraints on `@PathVariable` / `@RequestParam` (`@Positive int id`, `@Pattern` filters). Controllers are not annotated `@Validated`: Spring MVC 6.1 validates the method parameters itself (`HandlerMethodValidationException`).
- Use-case layer: every `*Ucc` interface is `@Validated` and declares the constraints of its parameters (`@NotNull @Valid`, `@Positive`, `@NotBlank`...), so the rules hold whoever calls the use case (a `ConstraintViolationException`, also answered with `VALIDATION_FAILED`). Parameter constraints are declared on the interface only (Bean Validation forbids redefining them in the implementation).
- Rules that need the database or the state (uniqueness, existence of a referenced entity, state of a mobility...) stay in the use cases and throw a `BusinessException` with an `ErrorCode`.
- Tests: `Violations.of(dto)` / `Violations.onCreate(dto)` list the broken constraints as `"field:Constraint"`, `Violations.thrownBy(() -> ucc.call(...))` those of a use case.

## Getting started
1. Inspect `src/main/resources/application.properties` for the database and JWT settings (see *Configuration* below).
2. Explore the DTOs and their constraints in the `business` package.
3. Examine the controllers in `web` for the available API endpoints and their roles (`@PreAuthorize`).
4. Review the SQL scripts under `SQLRessources` to understand the schema.
5. Run the JUnit tests under `src/test/java` (`mvn test`) as examples of typical workflows.

## Building
The project uses Maven for dependency management and builds. Execute:

```bash
mvn package
```

to compile the sources, run the tests and assemble the final JAR.

## Suggestions for further learning
- Follow a request from a `@RestController` to its `@Transactional` use case and the JDBC DAO.
- See how tests use the mock DAOs to isolate business logic.
- Investigate the front-end code in `src/main/webapp` to see how it interacts with the API.

## Frontend
The Angular client lives in the `frontend/` directory. To build it locally:

```bash
cd frontend
npm install
ng build
```

The compiled files will appear in `frontend/dist/`.

## Building with Maven
Run the standard Maven lifecycle to compile the sources, execute the tests and package the application:

```bash
mvn -B verify
```
The resulting JAR can be found under `target/`.


## Running with Docker Compose
A `docker-compose.yml` file is provided to run PostgreSQL (initialised with `SQLRessources/init.sql`), the Spring Boot backend and the Angular frontend.

To start everything:

```bash
docker compose up
```

The backend container exposes port `8080` (API and legacy web UI) while the Angular frontend is served on port `4200`.

### Configuration
The backend reads the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` (`db` in Compose) |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `testdb` |
| `DB_USERNAME` | Database user | _(empty)_ |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `JWT_SECRET` | Secret signing the session cookie, 32+ bytes | random per start (dev only) |
| `APP_SESSION_VALIDITY` | Lifetime of a session (JWT `exp` and cookie `Max-Age`), e.g. `12h`, `30m` | `12h` |
| `APP_SESSION_COOKIE_SECURE` | Adds `Secure` to the `session` and `XSRF-TOKEN` cookies (set it behind HTTPS) | `false` |
| `APP_CORS_ALLOWED_ORIGINS` | Origins allowed to call the API with credentials (comma-separated) | `http://localhost:4200` |

They are resolved in `src/main/resources/application.properties`. Always set `JWT_SECRET` outside local development, otherwise every restart logs everyone out.

## Continuous integration
`.github/workflows/ci.yml` runs on every pull request and on `master`:

| Job | What it checks |
|---|---|
| Backend | `mvn verify`: compile, JUnit 5 tests, JaCoCo coverage |
| Frontend | `npm ci`, production build, Karma tests (headless Chrome) with coverage |
| Workflow lint | `actionlint` on the workflows |
| Docker | `docker compose config`, backend and frontend image builds |
| Test report | publishes a **Test results** check (failures annotated on the diff), a job summary and a sticky PR comment with test counts and coverage |
| CI passed | single gate job to mark as *required* in branch protection |

Reproduce locally with `mvn verify` and `cd frontend && npm run test:ci`.

## Upgrade tasks
See [UPGRADE_TASKS.md](UPGRADE_TASKS.md) for the Spring migration status.
