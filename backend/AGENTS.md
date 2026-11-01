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
- **Framework**: Spring Boot 3
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Deployment**: Docker/Docker Compose
- **Testing**: JUnit 5 (Jupiter) with Mockito and Spring Boot Test

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
    └── java/.../persistence/mocks  # in-memory DAOs used by UnitTestConfig
pom.xml
```

## Development Guidelines

### Code Style

- Follow the Google Java Style Guide.
- Use the Maven formatter plugin before committing.
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

1. **Controllers** (`web`): one `@RestController` per resource under `/api/1.0` (`ApiPaths.BASE`). They only bind and validate the request (JSON `@Valid @RequestBody`, constrained `@PathVariable` / `@RequestParam`, the authenticated `CurrentUser`), call a use case and return its result (lists read by DataTables are wrapped in `DataResponse`, i.e. `{"data": [...]}`). Roles are checked with `@PreAuthorize(ApiPaths.PROFESSOR)` etc. Errors are rendered by `ApiExceptionHandler` only, as RFC 9457 problems.
2. **Use cases** (`uccontrollers`): `@Service` classes, annotated `@Transactional` at class level (queries `@Transactional(readOnly = true)`), without any web annotation. Their interfaces are `@Validated` and declare the constraints of the parameters. Nested use-case calls join the caller's transaction; any runtime exception rolls it back.
3. **DAOs** (`persistence`): see below.

```java
@RestController
@RequestMapping(ApiPaths.BASE + "/users")
public class UserController {
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<UserDto> showAll() {
    return new DataResponse<>(userUcc.showAll());
  }
}
```

Security (`config/SecurityConfig`, `security`): stateless. Sign-in (`POST /api/1.0/session`, JSON `{username, password}`) issues a signed HS256 JWT (`sub` = user id, `role`, `iat`, `exp`) in the HttpOnly, SameSite=Lax `session` cookie (`app.session.validity`, default 12h; `app.session.cookie-secure`, default false). The OAuth2 resource server validates it from the cookie or an `Authorization: Bearer` header. `/api/**` requires authentication except `POST /session`, `POST /users` and `GET /options`. CSRF uses the SPA recipe: the `XSRF-TOKEN` cookie must be echoed in the `X-XSRF-TOKEN` header of every state-changing request (`csrf()` in MockMvc tests).

### Feature Module 2

Persistence is Spring Data JPA (Hibernate), organised as hexagonal ports and adapters: see [ADR 0001](../docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md) for the decisions, the target layout and the next steps, and [the glossary](../GLOSSARY.md) for the vocabulary. The DAO interfaces (`persistence`) are the driven ports; each one is implemented by a package-private `@Repository` adapter in `adapter/out/persistence` (`XxxPersistenceAdapter`), which maps the JPA entities (`adapter/out/persistence/entity`, `XxxEntity`) to the DTOs with `PersistenceMapper` and queries through Spring Data interfaces (`adapter/out/persistence/repository`, `XxxJpaRepository`), entity graphs or JPQL built from constant fragments. Unit tests replace the DAOs with the in-memory mocks from `src/test/java/.../persistence/mocks` via `UnitTestConfig` (emptied before each test by `MockDaoResetListener`); the `*IT` tests run the real adapters against an embedded PostgreSQL (`DaoItConfig`, `AbstractDaoIT`).

Persistence rules:

- The schema is `SQLRessources/init.sql`: never generated (`ddl-auto=none`), validated by the ITs (`hbm2ddl.auto=validate`). Change it only when justified and agreed (issue #102, Q4).
- Associations are `LAZY`; a read fetches what its DTOs carry in **one** statement (`@EntityGraph` or `JOIN FETCH`). `QueryCountIT` fails on an N+1; add every new port read to it. `open-in-view` is off.
- Reference data (options, programmes, countries, documents) is `@Immutable`.
- An update loads the entity, compares the versions, copies the fields and flushes; creates with an assigned key flush at once.

DAO contracts (asserted by the `*IT` tests against PostgreSQL; the mocks follow them too):

- Every SQL failure is a Spring `DataAccessException` (exception translation of the `@Repository` adapters) with the `SQLException` as root cause; a constraint violation is a `DataIntegrityViolationException` (409 `DATA_CONFLICT`).
- An update that matches no row throws `ConcurrentModificationException`: stale version (optimistic locking) or unknown id. Updates give the DTO its new version; an update that changes nothing writes nothing and keeps the version (JPA dirty checking). `denial_reasons` has no version column, so `DenialReasonDao.update` only detects an unknown id.
- Optional relations are LEFT-joined and `null` when absent: a partner without option (empty `getOptions()`), a mobility choice / mobility / payment without partner or country.
- An unknown `findAll` filter (or `UserDao.findBy` column) is an `InvalidDataAccessApiUsageException` wrapping an `IllegalArgumentException`: a programming error, since the controllers and the `@Validated` use cases reject an unknown filter first (`@Pattern`, 400 `VALIDATION_FAILED` with the `filter` field in `errors`).
- A nominated student shares the id and the version of its user (`NominatedStudentDao.create` stores and returns the DTO's version).
- `UserDao.promoteToProfessor(int userId, int expectedVersion)` and `promoteToProfessor(String username, int expectedVersion)` write only the role and the version; `UserController`'s `PUT /api/1.0/users/{id}/promote` uses the id variant, `PUT /api/1.0/users/by-username/{username}/promote` the username one (both professors only, CSRF-protected; a professor is left unchanged). An unknown id or username is a 404 `RESOURCE_NOT_FOUND` problem, a stale version a 409 `CONCURRENT_MODIFICATION`, a blank or too long username a 400 `VALIDATION_FAILED`.
- The API leaves `null` properties out of the JSON (`NON_NULL`, `JacksonConfig`), as the legacy UI expects; the UI checks optional properties with `== null`, which also covers an explicit `null`.

### Errors, validation and messages

- **Errors** are RFC 9457 problems (`application/problem+json`) rendered by `web/ApiExceptionHandler` (a `ResponseEntityExceptionHandler`, the only place that formats errors; Spring Boot's own handler is disabled with `spring.mvc.problemdetails.enabled=false`). Members: `type` (`urn:pae:problem:<code>`), localized `title`/`detail`, `status`, `instance`, `code` (always, an `ErrorCode` name), `timestamp`, `errors` for `VALIDATION_FAILED`, `errorId` for 5xx (never any exception text). The full contract and the code table are in the README.
- **Business errors**: `throw new BusinessException(ErrorCode.X, args...)` (an `ErrorResponseException`). Adding a code: enum constant with its status (400 validation, 404 unknown URL resource, 409 state conflict, 422 business rule), then `problem.X.title` / `problem.X.detail` in **both** `i18n/messages.properties` (French, fallback) and `i18n/messages_en.properties`. Use `’`, not `'`, in messages taking arguments.
- **Persistence exceptions** are mapped by the handler, never caught in the use cases: `java.util.ConcurrentModificationException` and Spring Data's `OptimisticLockingFailureException` -> 409 `CONCURRENT_MODIFICATION`, `DataIntegrityViolationException` -> 409 `DATA_CONFLICT`, `EmptyResultDataAccessException` -> 404.
- **Validation**: constraints on the getters of the DTO interfaces (`business/dto`), custom ones in `business/validation`, groups `Default` / `OnCreate` / `Reference` (`ValidationGroups`). Controllers: `@Valid` bodies, constraints on path/query parameters (no class-level `@Validated`: Spring MVC 6.1 method validation). Use cases: `@Validated` interfaces with parameter constraints (declared on the interface only). Only rules needing the database or the state stay as code in the use cases.
- **i18n**: `MessageSource` basename `i18n/messages`, `fallback-to-system-locale=false`; Bean Validation messages resolve from it (`{key}` templates). Locale from `Accept-Language` (fr, en; default fr) via `AcceptLanguageLocaleResolver`, also applied by `ExceptionResolverSecurityHandler` to the 401/403 of the filters.
- **Tests**: `ProblemDetailsTest` (full problem bodies in fr/en), `MessageBundlesTest` (every key in both languages, every `ErrorCode` translated, every constraint message key translated), `MethodValidationTest` (use cases validated outside the web layer), `Violations` helper for the constraint tests.

### Business rules of the use cases

- **Mobility states**: `Créée` -> `En préparation` (a document is filled in) -> `A payer` (every departure document is filled in, also when the first document filled in completes them) -> `En cours` (first payment) -> `Solde à payer` (every document is filled in) -> `Terminée` (second payment); `Annulée` from any state but `Terminée`. `MobilityUcc.confirmPayment` accepts the first payment only in `A payer` and the second only in `Solde à payer`, anything else is a 409 `PAYMENT_NOT_EXPECTED`; when the return documents were already filled in during `A payer`, the first payment moves the mobility straight to `Solde à payer`.
- **Frozen mobilities**: a cancelled or closed mobility accepts no filled-in document (`confirmDocument`) and no payment: 409 `MOBILITY_CANCELLED` / `MOBILITY_CLOSED`. The software encodings are only refused on a cancelled mobility (a closed one may still be encoded afterwards).
- **Options of a partner** (`POST /api/1.0/partners/{id}`, `PartnerUcc.addOption`): a professor may add an option to any partner; a student only to a **non-official** partner that is the partner of one of his own mobility choices or mobilities (the `partners` table has no creator column; a student creates a non-official partner when he confirms a choice with a new partner). Anything else is a 403 `ACCESS_DENIED`, an unknown partner a 404 `RESOURCE_NOT_FOUND`. Official partners are managed by the professors only (as for `create`/`edit`).
- **Lists of a DTO** constrain their elements: `List<@NotNull @Valid PartnerOptionDto> getOptions()` reports a `null` option as `options[i]` / `NotNull` in a 400 `VALIDATION_FAILED` (at creation and at edition), so the use cases never see a `null` element.

## Testing Strategy

### Unit Testing

- Testing framework: JUnit 5 (`org.junit.jupiter`); use `assertThrows` instead of `@Test(expected = ...)`
- Test coverage requirements: the JaCoCo gates of `pom.xml` (`mvn verify`): overall 0.98 lines / 0.91 branches, persistence 0.98 / 0.98, use cases (`uccontrollers.implementations`) 0.98 / 0.97. Raise them as coverage grows, never lower them.
- Use cases: `*UccImplTest` test the business rules of a service with Mockito DAOs answering as their `*IT` contract says and the real business objects (`EntityFactories.create()`); `Test*Ucc` run the use cases in the `UnitTestConfig` context on the in-memory mock DAOs (emptied before each test by `MockDaoResetListener`). Assert on the returned DTO, the DAO writes and the exact error (`BusinessExceptionAssert`: `ErrorCode` and problem arguments).
- Test file organization: mirror package structure under `src/test/java`
- The stateful mock DAOs implement `ResettableMock`; `MockDaoResetListener` (registered for every Spring test in `src/test/resources/META-INF/spring.factories`) empties them before every test method, before the test's own `@BeforeEach`, so tests must not depend on each other. Check with `mvn test -Dsurefire.runOrder=random '-Djunit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random'`.
- Production code never writes to the console (`SourceHygieneTest`); log through SLF4J (`LoggerFactory.getLogger`).

### Integration Testing

- Test scenarios: every DAO method against the real schema (`*IT`, run by Failsafe in `mvn verify`)
- Testing tools: an embedded PostgreSQL (io.zonky, no Docker) loaded with `SQLRessources/init.sql`; `AbstractDaoIT` runs each test in a transaction that is always rolled back, with the fixtures of `src/test/resources/db/fixtures`

### End-to-End Testing

- Test workflow: executed via Docker Compose with the frontend
- Automation tools: Maven profiles

## Deployment Guide

### Build Process

```bash
mvn clean package
```

### Deployment Steps

1. Build the Docker image
2. Configure environment variables
3. Run `docker compose up backend`
4. Verify the API is reachable on port 8080

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

- Use Spring Boot Actuator for health checks
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

**Solution**: Unit tests should use `@SpringJUnitConfig(UnitTestConfig.class)` (mock DAOs, no database). Controller tests use `@WebMvcTest` with `@Import(WebTestConfig.class)` (real security chain, JSON mapping) and `@MockBean` use cases; authenticate with `TestUsers.professor()` / `student()` and add `csrf()` to state-changing requests. `@SpringBootTest` loads the real beans; it needs no database as long as the test does not hit a DAO.

## Reference Resources

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Maven Documentation](https://maven.apache.org/guides/index.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Changelog

### v1.0.0 (YYYY-MM-DD)

- Initial backend release
- Basic REST endpoints implemented

---
