---
name: "Erasmus-Management-Application Backend"
description: "Guidelines for the Spring Boot API."
category: "Backend Development"
author: "Dragomitch"
authorUrl: "https://github.com/Dragomitch"
tags: ["Java", "Spring Boot", "Maven", "PostgreSQL"]
lastUpdated: "2025-06-16"
---

# EMA Backend Guide

## Project Overview

This document provides instructions for working on the Java backend of the Erasmus Management Application. The backend exposes a REST API built with Spring Boot and handles business logic, persistence and security.

## Tech Stack

List the main technologies and tools used in the project:

- **Language**: Java 21
- **Framework**: Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Hibernate ORM 7, Jackson 3)
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Deployment**: Docker/Docker Compose
- **Testing**: JUnit 6 (Jupiter) with Mockito and Spring Boot Test

## Project Structure

```
backend/
├── Dockerfile
src/
├── main/
│   ├── java/
│   ├── resources/
│   └── webapp/
└── test/
    ├── java/       # mirrors the main package structure
    └── java/.../adapter/out/inmemory  # in-memory repositories used by UnitTestConfig
pom.xml
```

## Development Guidelines

### Code Style

- Follow the Google Java Style Guide. The layout is google-java-format's (2 spaces, 100 columns,
  one ASCII-sorted import block), applied by Spotless (`spotless-maven-plugin`, versions in the
  `spotless.version` and `google-java-format.version` properties of `pom.xml`) to
  `src/main/java` and `src/test/java`.
- Run `mvn spotless:apply` before committing; it formats the Java sources and removes unused
  imports. `mvn spotless:check` reports the files it would change without touching them, and it is
  bound to the `verify` phase, so `mvn verify` (the CI backend job) fails on a misformatted file.
- google-java-format refuses a comment at the end of an import line ("Imports not contiguous"):
  put the comment on its own line.
- Keep methods short and well-documented.

### Naming Conventions

- File naming: match the public class name.
- Variable naming: camelCase.
- Function naming: camelCase.
- Class naming: PascalCase.

### Git Workflow

- Branch from `master` using `feature/` or `bugfix/` prefixes.
- Write meaningful commit messages in English.
- Open a Pull Request with a summary of changes and link issues when relevant.

#### Pull requests

`.github/pull_request_template.md` pre-fills the description (Summary, Issue, Changes, Test plan, Screenshots); `.github/CODEOWNERS` requests the review. The conventions behind it:

- **Every change comes through a pull request** against `master`, and the PR links its issue with `Closes #n` so that merging closes it.
- The PR title is `<type>: <subject>` or `<type>(<scope>): <subject>`, with `<type>` one of `feat`, `fix`, `build`, `ci`, `docs`, `test`, `refactor`, `chore`, `perf`, `style` or `revert` (and an optional `!` before the colon for a breaking change); the `pr-title` job of the CI workflow fails on any other title, and the `CI passed` gate requires that job on pull requests.
- **The test plan states what was run and the numbers it produced**, never "should work": the exact command and its result, e.g. `mvn -B -ntp clean verify`: N unit tests, N integration tests, 0 failures, `All coverage checks have been met` (with the real numbers in place of N). Run the Maven build in default and in random order (`-Dsurefire.runOrder=random -Dfailsafe.runOrder=random`), `npm run test:ci` when the frontend changes and `docker compose config --quiet` when the Compose file or a Dockerfile does. Say what you could not run.
- **A PR that changes a screen embeds its screenshots.** Commit the images under `docs/screenshots/<topic>/` on the branch, then reference each one in the PR body as an HTML image wrapped in a link to its page, so that the image previews inline and opens in full on click:

  ```html
  <a href="https://github.com/Dragomitch/PAE-PostRemise/blob/<commit-sha>/docs/screenshots/<topic>/<file>.png"><img src="https://raw.githubusercontent.com/Dragomitch/PAE-PostRemise/<commit-sha>/docs/screenshots/<topic>/<file>.png" alt="alt text" width="700"></a>
  ```

  The HTML form, not the Markdown `[![alt](raw)](page)` one: the GitHub tooling the agents post through stores a Markdown image link wrapped in backticks, which renders as code (#162, #166 and #167 had to be rewritten), while `<a>` and `<img>` usually go through as written and render on GitHub and on Forgejo. That tooling also wraps some URLs in backticks on its own (the second and third images of #174, then their comments), so always follow each image with a plain line `Full size: <page URL>`: the link survives in every case, and a reviewer reaches the image in one click even when the preview is lost. One image per message is the safest; put further screenshots in one comment each. Use the full sha of a commit, not the branch name, so that the link keeps working after the branch is deleted. On a Forgejo mirror the image URL is `https://<host>/Dragomitch/PAE-PostRemise/raw/commit/<commit-sha>/docs/screenshots/<topic>/<file>.png` and the page is the same path under `/src/commit/<commit-sha>/`.

## Environment Setup

### Development Requirements

- Java 21
- Maven 3.9+
- A running PostgreSQL instance

### Installation Steps

```bash
# 1. Clone the project
git clone [repository-url]

# 2. Build and run tests
mvn verify

# 3. Start the application (dev profile)
mvn spring-boot:run
```

## Core Feature Implementation

### Feature Module 1

Requests flow through three layers, all Spring beans:

1. **Controllers** (`adapter/in/web`): one `@RestController` per resource under `/api/1.0` (`ApiPaths.BASE`). They only bind and validate the request (JSON `@Valid @RequestBody`, constrained `@PathVariable` / `@RequestParam`, the authenticated `CurrentUser`), call a use case and return its result (lists read by DataTables are wrapped in `DataResponse`, i.e. `{"data": [...]}`). Roles are checked with `@PreAuthorize(ApiPaths.PROFESSOR)` etc. Errors are rendered by `ApiExceptionHandler` only, as RFC 9457 problems.
2. **Use cases** (`domain/port/in` interfaces, `application` implementations): `@Service` classes, annotated `@Transactional` at class level (queries `@Transactional(readOnly = true)`), without any web annotation. Their interfaces are `@Validated` and declare the constraints of the parameters. Nested use-case calls join the caller's transaction; any runtime exception rolls it back.
3. **Repositories** (`domain/port/out` interfaces, `adapter/out/persistence` implementations): see below.

The layering is the hexagonal architecture of [ADR 0001](../docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md); `ArchitectureTest` (ArchUnit) fails the build when a dependency points outwards, a port is not an interface, or a port is implemented outside its adapter.

```java
@RestController
@RequestMapping(ApiPaths.BASE + "/users")
public class UserController {
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<UserDto> showAll() {
    return new DataResponse<>(userService.showAll());
  }
}
```

API documentation (`config/OpenApiConfig`, springdoc-openapi 3.x): the OpenAPI 3 document is generated from the controllers at `GET /v3/api-docs` (`/v3/api-docs.yaml`) and rendered by the Swagger UI at `/swagger-ui.html`; both are public in `SecurityConfig`. A new endpoint appears in the document by itself, with its parameters, constraints and JSON types (`OpenApiDocumentTest` fails when a mapping of `adapter.in.web` is missing from it); document it when its name is not enough: `@Tag` on a new controller, `@Operation(summary = ...)` on a method whose name does not say what it does (the state transitions, the exports), `@Schema(description = ...)` on a DTO getter whose meaning is not obvious (`term`, `preferenceOrder`, `version`). Do not repeat `@ApiResponse` on the methods: `OpenApiConfig` gives every operation its security (the `session` cookie unless the method has no `@PreAuthorize`, the `X-XSRF-TOKEN` header when it changes state) and its error responses (400, 401, 403, 404, 409, 422 as the operation can produce them), all referencing the `ProblemDetail` schema. `CurrentUser` is not a request parameter (`SpringDocUtils`), and `springdoc.paths-to-match=/api/**` keeps the web UI routes out.

Security (`config/SecurityConfig`, `adapter/in/web/security`): stateless. Sign-in (`POST /api/1.0/session`, JSON `{username, password}`) issues a signed HS256 JWT (`sub` = user id, `role`, `iat`, `exp`) in the HttpOnly, SameSite=Lax `session` cookie (`app.session.validity`, default 12h; `app.session.cookie-secure`, default false). The OAuth2 resource server validates it from the cookie or an `Authorization: Bearer` header. `/api/**` requires authentication except `POST /session`, `POST /users` and `GET /options`; the OpenAPI document (`/v3/api-docs/**`) and the Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`) are public. The security rules match paths case-insensitively, like the controllers: `SecurityConfig` declares the `PathPatternRequestMatcher.Builder` bean from `WebConfig.PATH_PATTERN_PARSER`, which replaces Spring Boot's case-sensitive default and also serves the `requestMatchers(String...)` shortcuts ([ADR 0002](../docs/adr/0002-spring-boot-4.md)). CSRF uses the SPA recipe: the `XSRF-TOKEN` cookie must be echoed in the `X-XSRF-TOKEN` header of every state-changing request (`csrf()` in MockMvc tests).

### Feature Module 2

Persistence is Spring Data JPA (Hibernate), organised as hexagonal ports and adapters: see [ADR 0001](../docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md) for the decisions, the target layout and the next steps, and [the glossary](../GLOSSARY.md) for the vocabulary. The `XxxRepository` interfaces (`domain/port/out`) are the driven ports; each one is implemented by a package-private `@Repository` adapter in `adapter/out/persistence` (`JpaXxxRepositoryAdapter`), which maps the JPA entities (`adapter/out/persistence/entity`, `XxxEntity`) to the DTOs with `PersistenceMapper` and queries through Spring Data interfaces (`adapter/out/persistence/repository`, `XxxJpaRepository`), entity graphs or JPQL built from constant fragments. Unit tests replace the repositories with the in-memory ones from `src/test/java/.../adapter/out/inmemory` via `UnitTestConfig` (emptied before each test by `InMemoryRepositoryResetListener`); the `*IT` tests run the real adapters against an embedded PostgreSQL (`RepositoryItConfig`, `AbstractRepositoryIT`).

Persistence rules:

- The schema is the Flyway migrations of `src/main/resources/db/migration` (see *Schema changes* below): never generated (`ddl-auto=none`), validated by the ITs (`hbm2ddl.auto=validate`). Change it only when justified and agreed (issue #102, Q4).
- Associations are `LAZY`; a read fetches what its DTOs carry in **one** statement (`@EntityGraph` or `JOIN FETCH`). `QueryCountIT` fails on an N+1; add every new port read to it. `open-in-view` is off.
- Reference data (options, programmes, countries, documents) is `@Immutable`.
- An update loads the entity, compares the versions, copies the fields and flushes with `Versions.flushIncrementingVersion`; creates with an assigned key flush at once.

#### Schema changes

The application migrates its database at start-up with Flyway ([ADR 0003](../docs/adr/0003-flyway-migrations.md)): `spring.flyway.*` in `application.properties`, migrations in `src/main/resources/db/migration`, history in `student_exchange_tools.flyway_schema_history`. `V1__baseline.sql` is the schema and reference data of `SQLRessources/init.sql`, and a database created from `init.sql` is adopted as version 1 (`baseline-on-migrate`). A schema change is one new migration, `V<n>__<description>.sql` (next free number, a short snake_case description), in the PR that changes the entities; never edit, rename or delete a merged migration (Flyway refuses to start on a checksum mismatch), and never edit `init.sql` or V1 (`FlywayBaselineIT` keeps them equal). Reference data the application needs goes in a migration; demo data stays in `SQLRessources/demo.sql`. The ITs run the migrations (`RepositoryItConfig`), so `mvn verify` checks the new migration against the entities. To reset a local database, drop the `student_exchange_tools` schema (or load `init.sql`) and restart the backend.

Repository contracts (asserted by the `*RepositoryIT` tests against PostgreSQL; the in-memory repositories follow them too):

- Every SQL failure is a Spring `DataAccessException` (exception translation of the `@Repository` adapters) with the `SQLException` as root cause; a constraint violation is a `DataIntegrityViolationException` (409 `DATA_CONFLICT`). Hibernate 7's `EntityManager.find(Class, Object)` returns `null` instead of throwing some of them (HHH-12472), so the Spring Data repositories are built on `StrictFindByIdRepository` (`JpaRepositoriesConfig`), and an adapter that loads by id through its `EntityManager` calls `StrictFindByIdRepository.find`.
- An update that matches no row throws `ConcurrentModificationException`: stale version (optimistic locking) or unknown id. Updates give the DTO its new version and increment it even when they change nothing (`Versions.flushIncrementingVersion`, `VersionContractIT`): the partner edit relies on it. `denial_reasons` has no version column, so `DenialReasonRepository.update` only detects an unknown id.
- Optional relations are LEFT-joined and `null` when absent: a partner without option (empty `getOptions()`), a mobility choice / mobility / payment without partner or country.
- An unknown `findAll` filter (or `UserRepository.findBy` column) is an `InvalidDataAccessApiUsageException` wrapping an `IllegalArgumentException`: a programming error, since the controllers and the `@Validated` use cases reject an unknown filter first (`@Pattern`, 400 `VALIDATION_FAILED` with the `filter` field in `errors`).
- A nominated student shares the id and the version of its user (`NominatedStudentRepository.create` stores and returns the DTO's version).
- `UserRepository.promoteToProfessor(int userId, int expectedVersion)` and `promoteToProfessor(String username, int expectedVersion)` write only the role and the version; `UserController`'s `PUT /api/1.0/users/{id}/promote` uses the id variant, `PUT /api/1.0/users/by-username/{username}/promote` the username one (both professors only, CSRF-protected; a professor is left unchanged). An unknown id or username is a 404 `RESOURCE_NOT_FOUND` problem, a stale version a 409 `CONCURRENT_MODIFICATION`, a blank or too long username a 400 `VALIDATION_FAILED`.
- The API leaves `null` properties out of the JSON (`NON_NULL`, `JacksonConfig`), as the legacy UI expects; the UI checks optional properties with `== null`, which also covers an explicit `null`. JSON is Jackson 3 (`tools.jackson`; the annotations stay `com.fasterxml.jackson.annotation`): a module is a `JacksonModule` bean and the mapper is customised with a `JsonMapperBuilderCustomizer`. A `null` sent for a primitive property is read as `0`, as with Jackson 2 (`spring.jackson.deserialization.fail-on-null-for-primitives=false`).

### Errors, validation and messages

- **Errors** are RFC 9457 problems (`application/problem+json`) rendered by `web/ApiExceptionHandler` (a `ResponseEntityExceptionHandler`, the only place that formats errors; Spring Boot's own handler is disabled with `spring.mvc.problemdetails.enabled=false`). Members: `type` (`urn:pae:problem:<code>`), localized `title`/`detail`, `status`, `instance`, `code` (always, an `ErrorCode` name), `timestamp`, `errors` for `VALIDATION_FAILED`, `errorId` for 5xx (never any exception text). The full contract and the code table are in the README.
- **Business errors**: `throw new BusinessException(ErrorCode.X, args...)` (an `ErrorResponseException`). Adding a code: enum constant with its status (400 validation, 404 unknown URL resource, 409 state conflict, 422 business rule), then `problem.X.title` / `problem.X.detail` in **both** `i18n/messages.properties` (French, fallback) and `i18n/messages_en.properties`. Use `’`, not `'`, in messages taking arguments.
- **Persistence exceptions** are mapped by the handler, never caught in the use cases: `java.util.ConcurrentModificationException` and Spring Data's `OptimisticLockingFailureException` -> 409 `CONCURRENT_MODIFICATION`, `DataIntegrityViolationException` -> 409 `DATA_CONFLICT`, `EmptyResultDataAccessException` -> 404.
- **Validation**: constraints on the getters of the DTO interfaces (`domain/dto`), custom ones in `domain/validation`, groups `Default` / `OnCreate` / `Reference` (`ValidationGroups`). Controllers: `@Valid` bodies, constraints on path/query parameters (no class-level `@Validated`: Spring MVC 6.1 method validation). Use cases: `@Validated` interfaces with parameter constraints (declared on the interface only). Only rules needing the database or the state stay as code in the use cases.
- **i18n**: `MessageSource` basename `i18n/messages`, `fallback-to-system-locale=false`; Bean Validation messages resolve from it (`{key}` templates). Locale from `Accept-Language` (fr, en; default fr) via `AcceptLanguageLocaleResolver`, also applied by `ExceptionResolverSecurityHandler` to the 401/403 of the filters.
- **Tests**: `ProblemDetailsTest` (full problem bodies in fr/en), `MessageBundlesTest` (every key in both languages, every `ErrorCode` translated, every constraint message key translated), `MethodValidationTest` (use cases validated outside the web layer), `Violations` helper for the constraint tests.

### Business rules of the use cases

- **Mobility states**: `Créée` -> `En préparation` (a document is filled in) -> `A payer` (every departure document is filled in, also when the first document filled in completes them) -> `En cours` (first payment) -> `Solde à payer` (every document is filled in) -> `Terminée` (second payment); `Annulée` from any state but `Terminée`. `MobilityService.confirmPayment` accepts the first payment only in `A payer` and the second only in `Solde à payer`, anything else is a 409 `PAYMENT_NOT_EXPECTED`; when the return documents were already filled in during `A payer`, the first payment moves the mobility straight to `Solde à payer`.
- **Frozen mobilities**: a cancelled or closed mobility accepts no filled-in document (`confirmDocument`) and no payment: 409 `MOBILITY_CANCELLED` / `MOBILITY_CLOSED`. The software encodings are only refused on a cancelled mobility (a closed one may still be encoded afterwards).
- **Options of a partner** (`POST /api/1.0/partners/{id}`, `PartnerService.addOption`): a professor may add an option to any partner; a student only to a **non-official** partner that is the partner of one of his own mobility choices or mobilities (the `partners` table has no creator column; a student creates a non-official partner when he confirms a choice with a new partner). Anything else is a 403 `ACCESS_DENIED`, an unknown partner a 404 `RESOURCE_NOT_FOUND`. Official partners are managed by the professors only (as for `create`/`edit`).
- **Lists of a DTO** constrain their elements: `List<@NotNull @Valid PartnerOptionDto> getOptions()` reports a `null` option as `options[i]` / `NotNull` in a 400 `VALIDATION_FAILED` (at creation and at edition), so the use cases never see a `null` element.

## Testing Strategy

### Unit Testing

- Testing framework: JUnit 6 (`org.junit.jupiter`); use `assertThrows` instead of `@Test(expected = ...)`
- Test coverage requirements: the JaCoCo gates of `pom.xml` (`mvn verify`): overall 0.98 lines / 0.91 branches, persistence 0.98 / 0.98, use cases (`application`) 0.98 / 0.97. Raise them as coverage grows, never lower them.
- Use cases: `*ServiceImplTest` test the business rules of a service with Mockito repositories answering as their `*IT` contract says and the real business objects (`EntityFactories.create()`); `Test*Service` run the use cases in the `UnitTestConfig` context on the in-memory repositories (emptied before each test by `InMemoryRepositoryResetListener`). Assert on the returned DTO, the DAO writes and the exact error (`BusinessExceptionAssert`: `ErrorCode` and problem arguments).
- Test file organization: mirror package structure under `src/test/java`
- The stateful in-memory repositories implement `ResettableRepository`; `InMemoryRepositoryResetListener` (registered for every Spring test in `src/test/resources/META-INF/spring.factories`) empties them before every test method, before the test's own `@BeforeEach`, so tests must not depend on each other. Check with `mvn test -Dsurefire.runOrder=random '-Djunit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random'`.
- Production code never writes to the console (`SourceHygieneTest`); log through SLF4J (`LoggerFactory.getLogger`).
- Production code never reads the current date or time from the system (`LocalDateTime.now()`, `LocalDate.now()`, `Year.now()`; `ArchitectureTest` fails the build): the use cases and the persistence adapters take the `java.time.Clock` bean of `config/ClockConfig` and call `LocalDateTime.now(clock)`. A unit test that asserts a date builds the service with `Clock.fixed(...)` and compares exactly (`MobilityServiceImplTest`, `UserServiceImplTest`, `MobilityChoiceServiceImplTest`); `UnitTestConfig` and `RepositoryItConfig` provide the system clock. The creation date of a mobility choice is the exception: the database sets it (`SELECT LOCAL DATETIME` in `JpaMobilityChoiceRepositoryAdapter.create`).

### Integration Testing

- Test scenarios: every repository method against the real schema (`*IT`, run by Failsafe in `mvn verify`)
- Testing tools: an embedded PostgreSQL (io.zonky, no Docker) migrated by Flyway with the application's migrations, then `db/it-setup.sql`; `AbstractRepositoryIT` runs each test in a transaction that is always rolled back, with the fixtures of `src/test/resources/db/fixtures`. `FlywayBaselineIT` compares V1 with `SQLRessources/init.sql` and checks the baseline of a database created from it.

### End-to-End Testing

- Test workflow: executed via Docker Compose with the frontend
- Automation tools: Maven profiles

## Deployment Guide

### Build Process

```bash
mvn clean package
```

### Deployment Steps

1. Build the Docker image (`backend/Dockerfile`: a layered image, the dependencies in layers below the application classes, so a code-only change rebuilds and pulls one small layer)
2. Configure environment variables
3. Run `docker compose up backend`
4. Verify the API is reachable on port 8080 (`curl http://localhost:8080/actuator/health` answers `"status":"UP"`)

### Environment Variables

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=                  # 32+ bytes, signs the session JWT
APP_SESSION_VALIDITY=12h     # session lifetime
APP_SESSION_COOKIE_SECURE=false  # true behind HTTPS
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
```

## Performance Optimization

### Backend Optimization

- Index frequently queried columns
- Cache heavy computations when possible
- Use connection pooling

## Security Considerations

### Data Security

- Validate all input data with Bean Validation constraints (DTOs, request parameters, use-case methods)
- Never put exception messages, SQL or stack traces in an error response: 5xx problems only carry an `errorId`
- Bind parameters (prepared statements) to avoid SQL injection; never concatenate input into SQL
- Sanitize user-facing output

### Authentication & Authorization

- Spring Security manages authentication (stateless JWT in the `session` cookie, OAuth2 resource server)
- `@PreAuthorize` role checks on every controller method (`ROLE_PROFESSOR`, `ROLE_STUDENT`)
- CSRF protection with the `XSRF-TOKEN` cookie / `X-XSRF-TOKEN` header
- 401 and 403 are rendered by `ApiExceptionHandler`, like every other API error (`UNAUTHENTICATED`, `INVALID_CREDENTIALS`, `ACCESS_DENIED` problems)

## Monitoring and Logging

### Application Monitoring

- Spring Boot Actuator exposes `health` and `info` over HTTP, nothing else (`management.endpoints.web.exposure.include`). `GET /actuator/health` (and `/actuator/health/liveness`, `/readiness`) is public and shows the status only, no component or detail: `200` with `"status":"UP"`, or `503` with `"status":"DOWN"` when the database is unreachable (the database indicator counts). Everything else under `/actuator` requires a session (`SecurityConfig`). The `backend` service of `docker-compose.yml` uses it as its `healthcheck`, and `frontend` waits for it to be healthy.
- The application stops gracefully (`server.shutdown=graceful`, 20 seconds for the requests in flight, `stop_grace_period: 30s` in Compose) and serves requests on virtual threads (`spring.threads.virtual.enabled=true`).
- Track errors via logs
- Optionally integrate with Prometheus/Grafana

### Log Management

- Log levels configured in `application.properties`
- Use a rolling file appender
- Store logs within the container or forward to a central system

## Common Issues

### Issue 1: Database connection failure

**Solution**: Verify environment variables and ensure the PostgreSQL container is running.

### Issue 2: Tests fail due to context loading

**Solution**: Unit tests should use `@SpringJUnitConfig(UnitTestConfig.class)` (in-memory repositories, no database). Controller tests use `@WebMvcTest` with `@Import(WebTestConfig.class)` (real security chain, JSON mapping) and `@MockitoBean` use cases; authenticate with `TestUsers.professor()` / `student()` and add `csrf()` to state-changing requests. The security chain is only in the slice because `spring-boot-starter-security-oauth2-resource-server-test` is on the test classpath (Spring Boot 4 moved that auto-configuration out of `spring-security-test`). `@SpringBootTest` loads the real beans; it needs no database as long as the test does not hit a DAO. `ApplicationTests` calls the running server with `RestTestClient` (`@AutoConfigureRestTestClient`).

## Reference Resources

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Maven Documentation](https://maven.apache.org/guides/index.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Changelog

### v1.0.0 (YYYY-MM-DD)

- Initial backend release
- Basic REST endpoints implemented

---
