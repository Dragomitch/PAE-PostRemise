# Erasmus Mobility Management

[![Build Status](https://travis-ci.org/Dragomitch/PAE-PostRemise.svg?branch=master)](https://travis-ci.org/Dragomitch/PAE-PostRemise)

This repository contains a web application for managing Erasmus mobilities. It was created as a second-year project at the Institut Paul Lambin during the 2015–2016 academic year.

## General structure
- **src/main/java**
  - `business` – entity interfaces and implementations (`User`, `Mobility`, …) with validation logic and DTO definitions.
  - `persistence` – DAO interfaces and implementations for database access as well as mocks for tests.
  - `uccontrollers` – use case controllers that orchestrate business logic and expose API routes.
  - `presentation` – a lightweight HTTP layer with a custom routing system.
  - `context` – utilities such as the dependency and context managers.
  - additional packages include `logging`, `exceptions`, `utils`, and the `main` entry point.
- **src/main/resources** contains configuration files (`dev.properties`, `errors.json`, SQL scripts…).
- **src/main/webapp** hosts the client-side HTML, CSS and JavaScript.
- **test/java** holds JUnit tests using mock DAOs.

## Key design aspects
- **Dependency injection**: implementations are resolved via reflection from `dev.properties`.
- **Servlet routing**: controllers expose routes through custom annotations that are processed at start-up.
- **Entry point**: `Main` loads the context, initializes error messages and starts a Jetty server.
- **Validation helpers**: common checks are centralised in `DataValidationUtils`.

## Getting started
1. Inspect `src/main/resources/dev.properties` to see how interfaces map to implementations and to configure the database.
2. Explore the DTOs and validation logic in the `business` package.
3. Examine the controllers in `uccontrollers` for available API endpoints (look for `@Route`).
4. Review the SQL scripts under `SQLRessources` to understand the schema.
5. Run the JUnit tests under `test/java` as examples of typical workflows.

## Building
The project uses Maven for dependency management and builds. Execute:

```bash
mvn package
```

to compile the sources, run the tests and assemble the final JAR.

## Suggestions for further learning
- Dive into the custom annotation-based routing and dependency injection system.
- See how tests use the mock DAOs to isolate business logic.
- Investigate the front-end code in `src/main/webapp` to see how it interacts with the API.

