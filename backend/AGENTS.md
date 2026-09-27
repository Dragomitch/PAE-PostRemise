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

Persistence is handled by JDBC DAOs under `persistence/implementations` (`@Repository`). They prepare their statements through `DalBackendServices`, which uses the connection of the current Spring transaction (`DataSourceUtils.getConnection`) and throws if no transaction is active. Updates check the entity version and throw a `ConcurrentModificationException` (answered with the 409 `CONCURRENT_MODIFICATION` problem) when it is stale. Unit tests replace the DAOs with the in-memory mocks from `src/test/java/.../persistence/mocks` via `UnitTestConfig` (emptied before each test by `MockDaoResetListener`); the `*IT` tests run the real DAOs against an embedded PostgreSQL.

### Errors, validation and messages

- **Errors** are RFC 9457 problems (`application/problem+json`) rendered by `web/ApiExceptionHandler` (a `ResponseEntityExceptionHandler`, the only place that formats errors; Spring Boot's own handler is disabled with `spring.mvc.problemdetails.enabled=false`). Members: `type` (`urn:pae:problem:<code>`), localized `title`/`detail`, `status`, `instance`, `code` (always, an `ErrorCode` name), `timestamp`, `errors` for `VALIDATION_FAILED`, `errorId` for 5xx (never any exception text). The full contract and the code table are in the README.
- **Business errors**: `throw new BusinessException(ErrorCode.X, args...)` (an `ErrorResponseException`). Adding a code: enum constant with its status (400 validation, 404 unknown URL resource, 409 state conflict, 422 business rule), then `problem.X.title` / `problem.X.detail` in **both** `i18n/messages.properties` (French, fallback) and `i18n/messages_en.properties`. Use `’`, not `'`, in messages taking arguments.
- **Persistence exceptions** are mapped by the handler, never caught in the use cases: `java.util.ConcurrentModificationException` and Spring Data's `OptimisticLockingFailureException` -> 409 `CONCURRENT_MODIFICATION`, `DataIntegrityViolationException` -> 409 `DATA_CONFLICT`, `EmptyResultDataAccessException` -> 404.
- **Validation**: constraints on the getters of the DTO interfaces (`business/dto`), custom ones in `business/validation`, groups `Default` / `OnCreate` / `Reference` (`ValidationGroups`). Controllers: `@Valid` bodies, constraints on path/query parameters (no class-level `@Validated`: Spring MVC 6.1 method validation). Use cases: `@Validated` interfaces with parameter constraints (declared on the interface only). Only rules needing the database or the state stay as code in the use cases.
- **i18n**: `MessageSource` basename `i18n/messages`, `fallback-to-system-locale=false`; Bean Validation messages resolve from it (`{key}` templates). Locale from `Accept-Language` (fr, en; default fr) via `AcceptLanguageLocaleResolver`, also applied by `ExceptionResolverSecurityHandler` to the 401/403 of the filters.
- **Tests**: `ProblemDetailsTest` (full problem bodies in fr/en), `MessageBundlesTest` (every key in both languages, every `ErrorCode` translated, every constraint message key translated), `MethodValidationTest` (use cases validated outside the web layer), `Violations` helper for the constraint tests.

## Testing Strategy

### Unit Testing

- Testing framework: JUnit 5 (`org.junit.jupiter`); use `assertThrows` instead of `@Test(expected = ...)`
- Test coverage requirements: aim for 80%
- Test file organization: mirror package structure under `src/test/java`

### Integration Testing

- Test scenarios: repository and controller integration
- Testing tools: Spring Boot Test with an in-memory database

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
- Use prepared statements to avoid SQL injection
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
