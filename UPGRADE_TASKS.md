# Spring Upgrade Tasks

This document tracks the migration of the project from standalone libraries to Spring Boot components.

| # | Task | Status |
|---|------|--------|
| 1 | Replace Genson with Jackson | Done (#55, #56). Spring MVC's `ObjectMapper` reads the DTO interfaces through the `EntityFactory` bindings (`JacksonConfig`). |
| 2 | Replace the Auth0 JWT library with Spring Security (`spring-security-oauth2-jose`) | Done (#55). Tokens are signed with HS256 from `JWT_SECRET`. |
| 3 | Use SLF4J/Logback instead of the custom `java.util.logging` setup | Done (#55). The `LogManager` wrapper is gone: classes use `LoggerFactory`. |
| 4 | Remove the Heroku deployment plugin | Done. |
| 5 | Replace jBCrypt with Spring Security's `PasswordEncoder` | Done (#55). |
| 6 | Replace `commons-dbcp2` with HikariCP | Done. The pool is now the Spring Boot `DataSource`. |
| 7 | Drop `commons-pool2` | Done. |
| 8 | Replace Hamcrest with JUnit Jupiter assertions | Done. The suite runs on JUnit 5. |
| 9 | Replace the custom `DependencyManager`/`ContextManager` with Spring dependency injection | Done. |
| 10 | Replace the custom web framework (`RoutingServlet`, `@Route`/`@Role` annotations, `Invoker`, `JsonSerializer`, `SessionManager`) with Spring MVC `@RestController`s and Spring Security | Done. Stateless JWT cookie resolved by the OAuth2 resource server, `@PreAuthorize` role checks, CSRF cookie for the SPAs, JSON request bodies. |
| 11 | Replace the `UnitOfWork` and `DalServices` transaction handling with Spring `@Transactional` | Done. DAOs use the transaction-bound connection (`DataSourceUtils`). |
| 12 | Replace the hand-written JDBC DAOs with Spring Data | In progress. Spring Data JDBC (not JPA: it maps the hand-written schema as it is, with no session or lazy loading, on the same transaction manager). Options, programmes, countries, documents, denial reasons, addresses and users use repositories (`persistence/jdbc`) behind the unchanged DAO interfaces. Payments and the join-heavy DAOs use `JdbcClient`, and `DalBackendServices` is gone. The DAO ITs pass unchanged. Next: Partner + PartnerOption, NominatedStudent, Mobility, MobilityChoice, then MobilityDocument after Spring Boot 3.5 (composite ids); plan in `backend/AGENTS.md`. |

## Possible next steps
- Render the API errors as RFC 9457 `ProblemDetail` (with i18n messages) in `ApiExceptionHandler`, the single place that formats every error, 401/403 included.
- Validate the request bodies with Bean Validation (`@Valid`) instead of the hand-written `checkDataIntegrity` methods.
- Finish the Spring Data JDBC migration (task 12) with the checklist of `backend/AGENTS.md`; the DAO integration tests (`*IT`) describe the behaviour to keep.
