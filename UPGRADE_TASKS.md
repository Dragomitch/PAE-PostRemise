# Spring Upgrade Tasks

This document tracks the migration of the project from standalone libraries to more modern Spring Boot components.

## 1. Replace Genson
- **Current library:** `com.owlike:genson`
- **Goal:** Use Jackson (already included via Spring Boot) for JSON serialization/deserialization.
- **Steps:**
  1. Remove the Genson dependency from `pom.xml`.
  2. Annotate entities with Jackson annotations (`@JsonProperty`, `@JsonIgnore`, etc.).
  3. Replace `JsonSerializer` implementations with Spring's `ObjectMapper`.
  4. Update any tests or utilities relying on Genson.

## 2. Replace JWT Library
- **Current library:** `com.auth0:java-jwt`
- **Goal:** Adopt Spring Security's JWT handling (e.g. `spring-security-oauth2-jose` or `jjwt`).
- **Steps:**
  1. Introduce Spring Security and configure JWT encoder/decoder beans.
  2. Refactor `SessionManager` to delegate token creation and validation to Spring Security.
  3. Remove the Auth0 dependency once migration is complete.

## 3. Update Logging
- **Current libraries:** custom `LogManager` with `java.util.logging` and `commons-logging` (unused).
- **Goal:** Use Spring Boot's default logging stack (SLF4J + Logback).
- **Steps:**
  1. Replace custom log manager with `org.slf4j.Logger` obtained via `LoggerFactory`.
  2. Remove the `commons-logging` dependency and related configuration.
  3. Configure log levels in `application.properties` or `logback-spring.xml`.

## 4. Remove Heroku Deployment Plugin
- **Current library:** `com.heroku.sdk:heroku-deploy`
- **Goal:** This project no longer deploys to Heroku.
- **Steps:**
  1. Delete the Heroku dependency from `pom.xml`.
  2. Clean up any scripts or documentation mentioning Heroku.

## 5. Replace jBCrypt
- **Current library:** `org.mindrot:jbcrypt`
- **Goal:** Use Spring Security's `PasswordEncoder` with a stronger algorithm (e.g. Argon2 or BCrypt implementation from Spring).
- **Steps:**
  1. Add `spring-security-crypto` and configure a `PasswordEncoder` bean.
  2. Refactor `UserUccImpl` and `SessionUccImpl` to use the encoder.
  3. Remove the `jbcrypt` dependency after migration.

## 6. Review `commons-codec` and `commons-dbcp2`
- **Usage:**
  - `commons-dbcp2` provides `BasicDataSource` used in `DalServicesImpl` for connection pooling.
  - `commons-codec` does not appear to be referenced in the codebase.
- **Replacement:**
  1. Replace `BasicDataSource` with Spring Boot's default connection pool (HikariCP via `spring-boot-starter-data-jpa`).
  2. Remove `commons-codec` if no direct usages remain.

## 7. Review `commons-pool2`
- **Usage:** Transitive dependency of `commons-dbcp2` for pooling connections.
- **Replacement:** Once HikariCP is adopted and `commons-dbcp2` removed, `commons-pool2` can also be dropped.

## 8. Replace Hamcrest
- **Current library:** `org.hamcrest:hamcrest-core`
- **Goal:** Use AssertJ (included with `spring-boot-starter-test`).
- **Steps:**
  1. Replace any Hamcrest assertions in tests with AssertJ or JUnit Jupiter assertions.
  2. Remove the Hamcrest dependency.

