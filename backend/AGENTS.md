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
- **Testing**: JUnit 4 with Spring Boot Test

## Project Structure

```
backend/
├── Dockerfile
src/
├── main/
│   ├── java/
│   ├── resources/
│   └── webapp/
└── test/java/
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

Persistence is handled with Spring Data JPA repositories found under `persistence`. Entities are mapped using standard JPA annotations.

## Testing Strategy

### Unit Testing

- Testing framework: JUnit 4
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

**Solution**: Check that `application-test.properties` points to an in-memory database and that migrations run before tests.

## Reference Resources

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Maven Documentation](https://maven.apache.org/guides/index.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Changelog

### v1.0.0 (YYYY-MM-DD)

- Initial backend release
- Basic REST endpoints implemented

---
