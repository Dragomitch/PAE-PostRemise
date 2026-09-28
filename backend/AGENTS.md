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

Business logic is organised in use case controllers located under `src/main/java/com/dragomitch/ipl/pae/uccontrollers`. Each controller exposes REST endpoints using Spring MVC annotations.

```java
// Example code
@RestController
@RequestMapping("/api/users")
public class UserController {
  @GetMapping
  public List<UserDto> listUsers() {
    // Implementation logic
  }
}
```

### Feature Module 2

Persistence is handled by JDBC DAOs under `persistence/implementations` (`@Repository`), sharing a thread-bound connection from `DalServices` on top of the Spring Boot `DataSource`. Unit tests replace them with the in-memory mocks from `src/test/java/.../persistence/mocks` via `UnitTestConfig`.

DAO contracts (asserted by the `*IT` tests against PostgreSQL; the mocks follow them too):

- Every statement is closed (try-with-resources) and every SQL failure is a `FatalException` carrying the `SQLException` as cause.
- An update that matches no row throws `ConcurrentModificationException`: stale version (optimistic locking) or unknown id. Updates give the DTO its new version. `denial_reasons` has no version column, so `DenialReasonDao.update` only detects an unknown id.
- Optional relations are LEFT-joined and `null` when absent: a partner without option (empty `getOptions()`), a mobility choice / mobility / payment without partner or country.
- An unknown `findAll` filter is a `BusinessException` (`INVALID_MOBILITY_CHOICE_FILTER_323`, `INVALID_PARTNER_FILTER_709`).
- A nominated student shares the id and the version of its user (`NominatedStudentDao.create` stores and returns the DTO's version).
- `UserDao.promoteToProfessor(int userId, int expectedVersion)` and `promoteToProfessor(String username, int expectedVersion)` write only the role and the version; the API route `PUT /users/{id}/promote` uses the id variant, `PUT /users/by-username/{username}/promote` the username one.
- Every `ErrorFormat` code must exist in `src/main/resources/errors.json` (`ErrorCatalogueTest`).

## Testing Strategy

### Unit Testing

- Testing framework: JUnit 5 (`org.junit.jupiter`); use `assertThrows` instead of `@Test(expected = ...)`
- Test coverage requirements: aim for 80%
- Test file organization: mirror package structure under `src/test/java`
- Use-case tests extend `AbstractUccTest`: the mock DAOs (`ResettableMock`) are reset before every test, so tests must not depend on each other. Check with `mvn test -Dsurefire.runOrder=random '-Djunit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random'`.
- Production code never writes to the console (`SourceHygieneTest`); log through `LogManager.getLogger` (SLF4J).

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
```

## Performance Optimization

### Backend Optimization

- Index frequently queried columns
- Cache heavy computations when possible
- Use connection pooling

## Security Considerations

### Data Security

- Validate all input data
- Use prepared statements to avoid SQL injection
- Sanitize user-facing output

### Authentication & Authorization

- Spring Security manages authentication
- Roles determine access to endpoints
- JWT tokens are used for stateless sessions

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

**Solution**: Unit tests should use `@SpringJUnitConfig(UnitTestConfig.class)` (mock DAOs, no database). `@SpringBootTest` loads the real beans; it needs no database as long as the test does not hit a DAO.

## Reference Resources

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Maven Documentation](https://maven.apache.org/guides/index.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Changelog

### v1.0.0 (YYYY-MM-DD)

- Initial backend release
- Basic REST endpoints implemented

---
