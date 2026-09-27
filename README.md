# Erasmus Mobility Management

[![CI](https://github.com/Dragomitch/PAE-PostRemise/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Dragomitch/PAE-PostRemise/actions/workflows/ci.yml)

This repository contains a web application for managing Erasmus mobilities. It was created as a second-year project at the Institut Paul Lambin during the 2015–2016 academic year.

## General structure
- **src/main/java**
  - `business` – entity interfaces and implementations (`User`, `Mobility`, …) with validation logic and DTO definitions.
  - `persistence` – DAO interfaces and their JDBC implementations.
  - `uccontrollers` – use case controllers that orchestrate business logic and expose API routes.
  - `presentation` – a lightweight HTTP layer with a custom routing system (`RoutingServlet`).
  - `config` – Spring configuration (security/JWT, web UI routing).
  - additional packages include `logging`, `exceptions` and `utils`; `Application` is the Spring Boot entry point.
- **src/main/resources** contains `application.properties` (Spring configuration) and `errors.json` (error catalogue).
- **src/main/webapp** hosts the legacy client-side HTML, CSS and JavaScript, packaged as static content and served at `/`.
- **src/test/java** holds the JUnit 5 tests. Unit tests run the real business and use-case beans against the in-memory mock DAOs (`persistence/mocks`, wired by `UnitTestConfig`); `ApplicationTests` boots the whole application without a database.

## Key design aspects
- **Dependency injection**: Spring (constructor injection). Business objects are created through `EntityFactory`, which binds each business/DTO interface to its implementation.
- **Servlet routing**: controllers expose routes through custom annotations that are processed at start-up; `RoutingServlet` serves them under `/api/1.0/*`.
- **Sessions**: a signed JWT cookie (`session`) backs the HTTP session; authorization is enforced per route with `@Role`.
- **Entry point**: `Application` (Spring Boot, embedded Tomcat).
- **Validation helpers**: common checks are centralised in `DataValidationUtils`.

## Getting started
1. Inspect `src/main/resources/application.properties` for the database and JWT settings (see *Configuration* below).
2. Explore the DTOs and validation logic in the `business` package.
3. Examine the controllers in `uccontrollers` for available API endpoints (look for `@Route`).
4. Review the SQL scripts under `SQLRessources` to understand the schema.
5. Run the JUnit tests under `src/test/java` (`mvn test`) as examples of typical workflows.

## Building
The project uses Maven for dependency management and builds. Execute:

```bash
mvn package
```

to compile the sources, run the tests and assemble the final JAR.

## Suggestions for further learning
- Dive into the custom annotation-based routing system.
- See how tests use the mock DAOs to isolate business logic.
- Investigate the front-end code in `src/main/webapp` to see how it interacts with the API.

## Frontend
The Angular client lives in the `frontend/` directory. To build it locally:

```bash
cd frontend
npm install
ng build
```

The compiled files will appear in `frontend/dist/`.

## Building with Maven
Run the standard Maven lifecycle to compile the sources, execute the tests and package the application:

```bash
mvn -B verify
```
The resulting JAR can be found under `target/`.


## Running with Docker Compose
A `docker-compose.yml` file is provided to run PostgreSQL (initialised with `SQLRessources/init.sql`), the Spring Boot backend and the Angular frontend.

To start everything:

```bash
docker compose up
```

The backend container exposes port `8080` (API and legacy web UI) while the Angular frontend is served on port `4200`.

### Configuration
The backend reads the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` (`db` in Compose) |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `testdb` |
| `DB_USERNAME` | Database user | _(empty)_ |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `JWT_SECRET` | Secret signing the session cookie, 32+ bytes | random per start (dev only) |

They are resolved in `src/main/resources/application.properties`. Always set `JWT_SECRET` outside local development, otherwise every restart logs everyone out.

## Continuous integration
`.github/workflows/ci.yml` runs on every pull request and on `master`:

| Job | What it checks |
|---|---|
| Backend | `mvn verify`: compile, JUnit 5 tests, JaCoCo coverage |
| Frontend | `npm ci`, production build, Karma tests (headless Chrome) with coverage |
| Workflow lint | `actionlint` on the workflows |
| Docker | `docker compose config`, backend and frontend image builds |
| Test report | publishes a **Test results** check (failures annotated on the diff), a job summary and a sticky PR comment with test counts and coverage |
| CI passed | single gate job to mark as *required* in branch protection |

Reproduce locally with `mvn verify` and `cd frontend && npm run test:ci`.

## Upgrade tasks
See [UPGRADE_TASKS.md](UPGRADE_TASKS.md) for the Spring migration status.
