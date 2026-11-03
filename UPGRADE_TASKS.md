# Spring Upgrade Tasks

This document tracks the migration of the project from standalone libraries to Spring Boot components.

| # | Task | Status |
|---|------|--------|
| 1 | Replace Genson with Jackson | Done (#55, #56). Spring MVC's `JsonMapper` (Jackson 3 since task 16) reads the DTO interfaces through the `EntityFactory` bindings (`JacksonConfig`). |
| 2 | Replace the Auth0 JWT library with Spring Security (`spring-security-oauth2-jose`) | Done (#55). Tokens are signed with HS256 from `JWT_SECRET`. |
| 3 | Use SLF4J/Logback instead of the custom `java.util.logging` setup | Done (#55). The `LogManager` wrapper is gone: classes use `LoggerFactory`. |
| 4 | Remove the Heroku deployment plugin | Done. |
| 5 | Replace jBCrypt with Spring Security's `PasswordEncoder` | Done (#55). |
| 6 | Replace `commons-dbcp2` with HikariCP | Done. The pool is now the Spring Boot `DataSource`. |
| 7 | Drop `commons-pool2` | Done. |
| 8 | Replace Hamcrest with JUnit Jupiter assertions | Done. The suite runs on JUnit Jupiter (JUnit 6 since task 16). |
| 9 | Replace the custom `DependencyManager`/`ContextManager` with Spring dependency injection | Done. |
| 10 | Replace the custom web framework (`RoutingServlet`, `@Route`/`@Role` annotations, `Invoker`, `JsonSerializer`, `SessionManager`) with Spring MVC `@RestController`s and Spring Security | Done. Stateless JWT cookie resolved by the OAuth2 resource server, `@PreAuthorize` role checks, CSRF cookie for the SPAs, JSON request bodies. |
| 11 | Replace the `UnitOfWork` and `DalServices` transaction handling with Spring `@Transactional` | Done. DAOs use the transaction-bound connection (`DataSourceUtils`). |
| 12 | Render the API errors as RFC 9457 problem details with i18n | Done. Spring `ProblemDetail` / `ErrorResponse` (`ApiExceptionHandler` extends `ResponseEntityExceptionHandler`), `ErrorCode` catalogue, messages in French and English (`i18n/messages*.properties`, `Accept-Language`). `errors.json`, `ErrorFormat` and `ErrorManager` are removed. |
| 13 | Replace the hand-written checks with Bean Validation | Done. Constraints on the DTO interfaces, on the request parameters and on the `@Validated` use cases; `checkDataIntegrity` and the `DataValidationUtils` checks are removed (`DataValidationUtils.isAValidString` remains for the mobility choice persistence adapter). |
| 14 | Replace the hand-written JDBC DAOs with Spring Data | Done (issue #102, step 1): Spring Data JPA adapters behind the DAO interfaces, `DalBackendServices` removed, N+1 guard (`QueryCountIT`). Decisions and contract changes in [ADR 0001](docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md). |
| 15 | Hexagonal packages and names (ports in `domain/port`, services in `application`, adapters in `adapter/in` and `adapter/out`) | Done (issue #102, step 2). `XxxDao` → `XxxRepository`, `XxxUcc` → `XxxService`, `XxxUccImpl` → `XxxServiceImpl`; `ArchitectureTest` (ArchUnit) enforces the layering. |
| 16 | Spring Boot 4 (Spring Framework 7, Spring Security 7, Hibernate ORM 7, Jackson 3, JUnit 6) | Done (issue #140): Spring Boot 4.1.2. Case-insensitive security matcher builder and Jackson 3 with the Jackson 2 behaviour for `null` primitives: see [ADR 0002](docs/adr/0002-spring-boot-4.md). |

## Possible next steps
- Hexagonal packaging and names (issue #102, step 2), then a pure domain model with web DTOs (step 3): see [ADR 0001](docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md).
- Translate the legacy web UI (`src/main/webapp`): only the messages coming from the API are localized today.
