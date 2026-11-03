# ADR 0002: Spring Boot 4

- **Status:** accepted, implemented.
- **Discussion:** issue #140 (supersedes the Dependabot PR #113); issue #102, Q5 of
  [ADR 0001](0001-spring-data-jpa-and-hexagonal-architecture.md) planned it as its own PR.

## Context

The backend moved from Spring Boot 3.5.16 to 4.1.2, which brings Spring Framework 7, Spring
Security 7.1, Hibernate ORM 7.4, Jackson 3 and JUnit 6. Most of the upgrade is mechanical: renamed
starters, auto-configuration classes in new packages, `tools.jackson` instead of
`com.fasterxml.jackson.databind`, JSpecify's `@Nullable`. Two changes alter behaviour without a
compile error, and each needed a decision.

1. **Security path matching.** The controllers match paths case-insensitively (`WebConfig`): the
   legacy UI calls `/mobilityChoices/...` for routes declared `/mobilitychoices/...`. Spring
   Security 7 removed `MvcRequestMatcher`, which used to match like the controllers. Spring Boot 4
   declares a `PathPatternRequestMatcher.Builder` bean, built with the default (case-sensitive)
   parser, and Spring Security's `requestMatchers(String...)` uses it. Left alone,
   `GET /API/1.0/users` would skip the `/api/**` rule (so no authentication would be required)
   while the controllers still serve it.
2. **Jackson 3 defaults.** Jackson 3 refuses a `null` for a primitive property
   (`FAIL_ON_NULL_FOR_PRIMITIVES` is on), and Spring Boot 4 does not turn it off. The DTO
   implementations have `int` properties (ids, term, academic year, version), and the legacy forms
   may send `null` for them: with Jackson 2 that was read as `0`, with Jackson 3 it would be a 400.
   Jackson 3 also sorts properties alphabetically by default.

## Decision

### One case-insensitive request matcher builder for Security

`SecurityConfig` declares the `PathPatternRequestMatcher.Builder` bean itself, from
`WebConfig.PATH_PATTERN_PARSER`, the parser Spring MVC uses for the controllers. Spring Boot's
default builder is `@ConditionalOnMissingBean`, so this one replaces it. `publicApiEndpoints`
builds its matchers from it, and the `requestMatchers(String...)` shortcuts of the DSL resolve
through it. There is one parser, so a path the controllers serve is always matched by the security
rules. `MvcRequestMatcher` and `HandlerMappingIntrospector` are no longer used.

Two tests guard it: `GET /API/1.0/users` without a session is a 401 (`ApplicationTests`,
`EndpointAuthorizationTest`), and a stale session cookie on `POST /api/1.0/SESSION` does not
turn the sign-in into a 401 (`SessionSecurityTest`).

The CSRF object post-processor of `SecurityConfig` stays as it was: only its import moved
(`org.springframework.security.config.ObjectPostProcessor`).

### Jackson 3, with the Jackson 2 behaviour for null primitives

`JacksonConfig` uses Jackson 3: the `EntityFactory` abstract-type mappings are a `JacksonModule`
bean (Spring Boot 4 ignores a Jackson 2 `Module` bean without an error), and `NON_NULL` is set by a
`JsonMapperBuilderCustomizer` with `changeDefaultPropertyInclusion`.

`application.properties` sets `spring.jackson.deserialization.fail-on-null-for-primitives=false`,
so a `null` for a primitive is still read as `0` (`JacksonConfigTest`). The broader
`spring.jackson.use-jackson2-defaults` is not used: the other Jackson 3 defaults are kept.

Alphabetical property order is accepted. No client or test depends on the order of the keys
(the tests compare JSON regardless of order, and the CSV exports do not use Jackson), and
`/actuator/health` now answers `{"groups":[...],"status":"UP"}`.

## Consequences

- `/api/**` stays protected whatever the case of the path, and the security rules follow any
  change to the MVC parser.
- The JSON contract is unchanged for the web UIs: `NON_NULL`, ISO dates, numbers sent as strings,
  `null` primitives read as `0`. Only the order of the keys changed.
- Hibernate 7's `EntityManager.find(Class, Object)` returns `null`, instead of throwing, when its
  `SELECT` fails with a generic JDBC or connection error (it marks the transaction for rollback,
  then applies HHH-12472). `RepositoryErrorHandlingIT` caught it on the updates and the finders by
  id. The Spring Data repositories are therefore built on `StrictFindByIdRepository`
  (`JpaRepositoriesConfig`), whose `findById` uses the JPA 3.2 `find(Class, Object, FindOption...)`,
  which lets the error through; `JpaMobilityChoiceRepositoryAdapter` uses the same helper.
- The tests use the Spring Boot 4 test starters (`spring-boot-starter-webmvc-test`,
  `spring-boot-starter-security-oauth2-resource-server-test`), which put the security filter
  chain in the `@WebMvcTest` slices, and `RestTestClient` instead of `TestRestTemplate`.
