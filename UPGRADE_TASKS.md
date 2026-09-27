# Spring Upgrade Tasks

This document tracks the migration of the project from standalone libraries to Spring Boot components.

| # | Task | Status |
|---|------|--------|
| 1 | Replace Genson with Jackson | Done (#55, #56). Scalar request parameters and interface-typed properties are handled by `JsonSerializer`. |
| 2 | Replace the Auth0 JWT library with Spring Security (`spring-security-oauth2-jose`) | Done (#55). Tokens are signed with HS256 from `JWT_SECRET`. |
| 3 | Use SLF4J/Logback instead of the custom `java.util.logging` setup | Done (#55). `LogManager` is now a thin SLF4J wrapper. |
| 4 | Remove the Heroku deployment plugin | Done. |
| 5 | Replace jBCrypt with Spring Security's `PasswordEncoder` | Done (#55). |
| 6 | Replace `commons-dbcp2` with HikariCP | Done. The pool is now the Spring Boot `DataSource`. |
| 7 | Drop `commons-pool2` | Done. |
| 8 | Replace Hamcrest with JUnit Jupiter assertions | Done. The suite runs on JUnit 5. |
| 9 | Replace the custom `DependencyManager`/`ContextManager` with Spring dependency injection | Done. `RoutingServlet` is registered by `PresentationConfig`. |

## Possible next steps
- Move the legacy `@Route` controllers to Spring MVC `@RestController`s, one collection at a time.
- Replace the hand-written JDBC DAOs with `JdbcTemplate` or Spring Data.
- Remove the `LogManager` wrapper in favour of `LoggerFactory` directly.
