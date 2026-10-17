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
- **Persistence**: Spring Data JDBC repositories and `JdbcClient` on PostgreSQL (schema in `SQLRessources/init.sql`)
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

1. **Controllers** (`web`): one `@RestController` per resource under `/api/1.0` (`ApiPaths.BASE`). They only bind the request (JSON `@RequestBody`, `@PathVariable`, `@RequestParam`, the authenticated `CurrentUser`), call a use case and return its result (lists read by DataTables are wrapped in `DataResponse`, i.e. `{"data": [...]}`). Roles are checked with `@PreAuthorize(ApiPaths.PROFESSOR)` etc. Errors are rendered by `ApiExceptionHandler` only.
2. **Use cases** (`uccontrollers`): `@Service` classes, annotated `@Transactional` at class level (queries `@Transactional(readOnly = true)`), without any web annotation. Nested use-case calls join the caller's transaction; any runtime exception rolls it back.
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

Persistence (`persistence`) is being migrated from hand-written JDBC to **Spring Data JDBC**. The use cases only see the DAO interfaces (`persistence/*Dao`), whose signatures and behaviour do not change; the `*IT` tests describe that behaviour and must keep passing unchanged.

**Why Spring Data JDBC rather than JPA.** The schema is hand-written SQL (`SQLRessources/init.sql`) and the DAOs return detached DTOs built by `EntityFactory`. Spring Data JDBC maps such a schema as it is (explicit `@Table`/`@Column`, no DDL generation), runs one SQL statement per call, and has no persistence context, lazy loading, dirty checking or flush timing. It runs on the same `DataSource` and `DataSourceTransactionManager` as the remaining SQL, so a use case can mix both in one transaction. JPA would need a `JpaTransactionManager` for everything and bring proxies that fail outside the session and entity-graph tuning, for no gain: the DTOs are copied anyway.

Target architecture:

```
persistence/
├── XxxDao.java                  # contract used by the use cases (unchanged)
├── implementations/XxxDaoImpl   # @Repository adapter: repository or JdbcClient -> DTO
│   └── DataAccess               # wraps every DAO call: transaction required, exception contract
└── jdbc/
    ├── JdbcPersistenceConfig    # AbstractJdbcConfiguration + @EnableJdbcRepositories, PostgreSQL dialect
    ├── entity/XxxEntity         # immutable records: @Table(schema = SCHEMA, name), @Id, @Column, @Version,
    │                            #   other aggregates referenced by id (AggregateReference)
    └── repository/XxxRepository # ListCrudRepository + derived queries, @Query/@Modifying for the rest
```

- **Transactions**: repositories and `JdbcClient` run on the `NamedParameterJdbcTemplate`, i.e. on the connection that `DataSourceUtils` binds to the Spring transaction opened by `@Transactional` on the use case. `DataAccess.call` rejects a DAO call outside a transaction (`IllegalStateException`). `SharedTransactionIT` checks that Spring Data and `JdbcClient` run in the same PostgreSQL transaction, see each other's uncommitted rows, and that the rollback undoes both.
- **Exceptions**: `DataAccess.call` keeps the DAO contract. `OptimisticLockingFailureException` becomes `ConcurrentModificationException` (error 120). Any other `DataAccessException`, and Spring Data's `DbActionExecutionException`, becomes `FatalException`, whose cause is the driver's `SQLException` (found in the cause chain) or, without one, the Spring exception. A 0-row partial update (`@Modifying @Query` or `JdbcClient.update()`) is reported by the adapter itself as `ConcurrentModificationException`.
- **Optimistic locking**: tables with a `version` column map it to a primitive `@Version int version`. Spring Data inserts 1 and updates with `WHERE id = ? AND version = ?`, incrementing it. Adapters call `JdbcAggregateOperations.insert`/`update` explicitly rather than `save`, which guesses from the version, and copy the new version back into the DTO. Read-only reference tables (countries, documents) do not map their unused version column.
- **Mapping**: entity <-> DTO mapping lives in the adapter (`toDto`/`toEntity`, a few lines). Names of referenced aggregates are read from that aggregate's repository instead of a join (the option name of a user, the country name of an address, the programme of a country): one extra query, or one `findAll` for a list.
- **Configuration**: `JdbcPersistenceConfig` replaces Spring Boot's Spring Data JDBC auto-configuration. The application and `DaoItConfig` (the IT context, which has no Boot auto-configuration) therefore use the same setup. The dialect is declared, so the application starts without a database.

| DAO | Implementation | Notes |
|---|---|---|
| Option, Programme, Country, Document | Spring Data repositories | read-only reference data; derived queries (`findAllByOrderByNameAsc`, `findByProgramme(AggregateReference)`) |
| DenialReason | Spring Data repository | no version: update is a `@Modifying @Query`; 0 rows (unknown id) is a `ConcurrentModificationException` |
| Address, User | Spring Data repositories + `JdbcAggregateOperations` | `@Version`; the user registration date is `@InsertOnlyProperty`; `promoteToProfessor` (by id or by username) is a `@Modifying @Query` `SET role, version = version + 1 WHERE ... AND version = :expectedVersion`: 1 row gives the new version `expectedVersion + 1`, 0 rows a `ConcurrentModificationException`; `findBy(column, value)` resolves the column through the mapping instead of concatenating it into the SQL |
| Payment | `JdbcClient` | read model (UNION over mobilities), not an aggregate; country and partner LEFT-joined |
| MobilityChoice, Mobility, MobilityDocument, NominatedStudent, Partner, PartnerOption | `JdbcClient` + `RowMapper` (a `ResultSetExtractor` for the partners: one row per option, merged per partner) | SQL kept (text blocks, explicit joins, LEFT JOIN for optional relations); `UPDATE ... WHERE version = ? RETURNING version` |

**Migrating the next DAO (checklist)**

1. Read its `*IT` (the contract, including the legacy behaviours it pins) and its table in `init.sql`.
2. Add `persistence/jdbc/entity/XxxEntity`: a record with `@Table(schema = JdbcPersistenceConfig.SCHEMA, name = ...)` and `@Id`. Add `@Column` for names that are not the snake_case of the property, a primitive `@Version int version` if the table has a version column, `AggregateReference` for foreign keys to other aggregates, and `@InsertOnlyProperty` for columns the old UPDATE did not write.
3. Add `persistence/jdbc/repository/XxxRepository extends ListCrudRepository<XxxEntity, Id>`. Prefer derived queries. Use `@Query` (schema spelled out) for joins and projections, and `@Modifying @Query` for partial updates that must not touch the version.
4. Rewrite `XxxDaoImpl` as an adapter: inject the repository through the constructor, plus `JdbcAggregateOperations` for version-checked writes. Wrap **every** method body in `DataAccess.call`/`run`, map with small `toDto`/`toEntity` methods, and use `DataAccess.reference(id)` for nullable references.
5. Run `mvn verify`. The DAO's `*IT`, `DaoErrorHandlingIT` (a `FatalException` with its `SQLException` cause from every method; no statement left open and no connection taken outside the transaction, as recorded by the `StatementRecordingDataSource` of `DaoItConfig`) and `SharedTransactionIT` must pass **unchanged**. Add repository-level tests to `JdbcRepositoriesIT` for new `@Query`/derived methods and `@Version` behaviour. The JaCoCo rule (0.98 lines/branches) covers `persistence.implementations` and `persistence.jdbc*`.
6. If a legacy behaviour must change, change the IT in the same commit, with a comment explaining why.

**What remains (on `JdbcClient`) and the plan for each**

- **Partner + PartnerOption**: one aggregate. `PartnerEntity` has a `@Version` and an `AggregateReference` to the address. It owns a `@MappedCollection(idColumn = "partner_id", keyColumn = "option_code") Map<String, PartnerOptionEntity>`, which handles the composite key of `partner_options`. Caveat: Spring Data rewrites the collection on every save and bumps the partner version, so `PartnerOptionDao.create` would increment the partner version. Decide that with the use-case owner, or keep that insert on `JdbcClient`. The filtered list (joins to address/country/programme, options LEFT-joined so that a partner without option is listed, the student option filter as an `EXISTS`) becomes a `@Query` returning a `PartnerView` record.
- **MobilityDocument**: composite key (document_id, mobility_id) with its own version. It can become a `@MappedCollection(idColumn = "mobility_id")` of the Mobility aggregate. The cleaner option is an entity with an embedded composite `@Id`, supported by Spring Data Relational 3.5 (Spring Boot 3.5) according to its release notes (check before relying on it): migrate it after that upgrade.
- **NominatedStudent**: shares its primary key with `users` (an assigned id, not generated): an entity with `@Id Integer userId`, written with `JdbcAggregateOperations.insert`. Its create stores and returns the DTO's version (the user's), which conflicts with `@Version` (which inserts 1): keep a `@Modifying @Query` insert. Reads join users/options/countries: a `@Query` into a `NominatedStudentView` record.
- **Mobility**: the primary key is `mobility_choice_id` (assigned), with a `@Version` and optional references to the denial reason and the professor (nullable `AggregateReference`s). Reads join six tables: a `@Query` into a `MobilityView` record, and `findByUser` as a second `@Query`.
- **MobilityChoice**: generated id and `@Version`. The database sets `submission_date` (`NOW()`) on insert, but update writes it. It needs a custom insert (`@Modifying @Query ... RETURNING`), or the date set in Java (a behaviour change). The filters ("active" and "passed" compare with the current year, "not yet a mobility") become `@Query` methods that take the year as a parameter, which also makes them testable without the clock.

Unit tests replace the DAOs with the in-memory mocks from `src/test/java/.../persistence/mocks` via `UnitTestConfig` (emptied before each test by `MockDaoResetListener`). The `*IT` tests run the real DAOs, repositories and `JdbcClient` against an embedded PostgreSQL (`DaoItConfig`, with the rolled-back transactions of `AbstractDaoIT`).

DAO contracts (asserted by the `*IT` tests against PostgreSQL; the mocks follow them too):

- Every statement is closed (by the `JdbcTemplate` under the repositories and `JdbcClient`; `DaoErrorHandlingIT` checks it through the recording data source) and every SQL failure is a `FatalException` carrying the `SQLException` as cause.
- An update that matches no row throws `ConcurrentModificationException`: stale version (optimistic locking) or unknown id. Updates give the DTO its new version. `denial_reasons` has no version column, so `DenialReasonDao.update` only detects an unknown id.
- Optional relations are LEFT-joined and `null` when absent: a partner without option (empty `getOptions()`), a mobility choice / mobility / payment without partner or country.
- An unknown `findAll` filter is a `BusinessException` (`INVALID_MOBILITY_CHOICE_FILTER_323`, `INVALID_PARTNER_FILTER_709`).
- A nominated student shares the id and the version of its user (`NominatedStudentDao.create` stores and returns the DTO's version).
- `UserDao.promoteToProfessor(int userId, int expectedVersion)` and `promoteToProfessor(String username, int expectedVersion)` write only the role and the version; `UserController`'s `PUT /api/1.0/users/{id}/promote` uses the id variant, `PUT /api/1.0/users/by-username/{username}/promote` the username one (both professors only, CSRF-protected; a professor is left unchanged).
- Every `ErrorFormat` code must exist in `src/main/resources/errors.json` (`ErrorCatalogueTest`): `ApiExceptionHandler` answers a `BusinessException` (e.g. an unknown filter, 323/709) and a `ConcurrentModificationException` (120, "reload the data") with 400 and the catalogue entry.
- The API leaves `null` properties out of the JSON (`NON_NULL`, `JacksonConfig`), as the legacy UI expects; the UI checks optional properties with `== null`, which also covers an explicit `null`.

## Testing Strategy

### Unit Testing

- Testing framework: JUnit 5 (`org.junit.jupiter`); use `assertThrows` instead of `@Test(expected = ...)`
- Test coverage requirements: aim for 80%
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

- Validate all input data
- Bind parameters (Spring Data, `JdbcClient`) to avoid SQL injection; never concatenate input into SQL
- Sanitize user-facing output

### Authentication & Authorization

- Spring Security manages authentication (stateless JWT in the `session` cookie, OAuth2 resource server)
- `@PreAuthorize` role checks on every controller method (`ROLE_PROFESSOR`, `ROLE_STUDENT`)
- CSRF protection with the `XSRF-TOKEN` cookie / `X-XSRF-TOKEN` header
- 401 and 403 are rendered by `ApiExceptionHandler`, like every other API error

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
