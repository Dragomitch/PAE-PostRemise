# Erasmus Mobility Management

[![CI](https://github.com/Dragomitch/PAE-PostRemise/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Dragomitch/PAE-PostRemise/actions/workflows/ci.yml)

This repository contains a web application for managing Erasmus mobilities. It was created as a second-year project at the Institut Paul Lambin during the 2015–2016 academic year.

## General structure
- **src/main/java**
  - `business` – entity interfaces and implementations (`User`, `Mobility`, …) with validation logic and DTO definitions.
  - `persistence` – DAO interfaces and their JDBC implementations.
  - `uccontrollers` – use cases (`@Service`, `@Transactional`) that orchestrate the business logic and the DAOs.
  - `web` – the REST API: one Spring MVC `@RestController` per resource under `/api/1.0`, and `ApiExceptionHandler` which renders every API error.
  - `security` – session JWT cookie, current user, CSRF helpers used by the security chain.
  - `config` – Spring configuration (security chain, JSON mapping, CORS, web UI routing).
  - additional packages include `exceptions` and `utils`; `Application` is the Spring Boot entry point.
- **src/main/resources** contains `application.properties` (Spring configuration) and `errors.json` (error catalogue).
- **src/main/webapp** hosts the legacy client-side HTML, CSS and JavaScript, packaged as static content and served at `/`.
- **src/test/java** holds the JUnit 5 tests. Unit tests run the real business and use-case beans against the in-memory mock DAOs (`persistence/mocks`, wired by `UnitTestConfig`); the `web` tests are `@WebMvcTest` slices with the real security chain and mocked use cases; `ApplicationTests` boots the whole application without a database; the `*IT` DAO tests run against an embedded PostgreSQL.

## Key design aspects
- **Layers**: controllers (`web`) → use cases (`uccontrollers`) → DAOs (`persistence`), all Spring beans wired by constructor injection. Business objects are created through `EntityFactory`, which binds each business/DTO interface to its implementation; the same bindings tell Spring MVC's Jackson `ObjectMapper` how to read the DTO interfaces (`JacksonConfig`).
- **REST API**: thin `@RestController`s bind the request (JSON body, path and query parameters, `CurrentUser`), call a use case and return its result. Paths are matched case-insensitively (as the former router did). Lists read by DataTables are wrapped in `{"data": [...]}`; exports are `text/csv`.
- **Transactions**: every use-case service is `@Transactional` (read-only for queries); the DAOs prepare their statements on the connection of the current Spring transaction (`DataSourceUtils`) and refuse to run outside one. Updates check the entity version (`WHERE ... AND version = ?`) and throw a `ConcurrentModificationException` (error 120) on a stale version.
- **Security** (Spring Security, stateless, see `SecurityConfig`):
  - `POST /api/1.0/session` with `{"username", "password"}` issues an HS256 JWT (`sub` = user id, `role`, `iat`, `exp`) in the `session` cookie: `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure` when `APP_SESSION_COOKIE_SECURE=true`, valid `APP_SESSION_VALIDITY` (12h). `GET` returns the current user, `DELETE` expires the cookie. No HTTP session is created.
  - The OAuth2 resource server validates the token from the cookie (or an `Authorization: Bearer` header); the `role` claim becomes `ROLE_PROFESSOR` / `ROLE_STUDENT`, checked by `@PreAuthorize` on the controllers. Every `/api/**` endpoint requires a session except sign-in, sign-up (`POST /users`) and the option list (`GET /options`).
  - CSRF: the `XSRF-TOKEN` cookie (readable by JavaScript) is issued on every response and must be echoed in the `X-XSRF-TOKEN` header of every `POST`/`PUT`/`DELETE`, sign-in included (Angular does it natively, `app.js` in `$.ajaxSetup`). Requests authenticated by an `Authorization` header are exempt.
  - Errors (401, 403 included) use the error catalogue format `{errorCode, developerMessage, userMessage, details}` (`ApiExceptionHandler`, `errors.json`).
- **Entry point**: `Application` (Spring Boot, embedded Tomcat).
- **Validation helpers**: common checks are centralised in `DataValidationUtils`.

## Getting started
1. Inspect `src/main/resources/application.properties` for the database and JWT settings (see *Configuration* below).
2. Explore the DTOs and validation logic in the `business` package.
3. Examine the controllers in `web` for the available API endpoints and their roles (`@PreAuthorize`).
4. Review the SQL scripts under `SQLRessources` to understand the schema. A database created from an older `init.sql` gets the countries added since with `SQLRessources/add-missing-countries.sql` (idempotent).
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

## Roadmap
See [docs/ROADMAP.md](docs/ROADMAP.md) for the CI/CD proposals, the Spring / Spring Boot improvements and the behaviour decisions.

## Upgrade tasks
See [UPGRADE_TASKS.md](UPGRADE_TASKS.md) for the Spring migration status.
