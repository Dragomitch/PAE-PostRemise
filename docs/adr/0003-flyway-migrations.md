# ADR 0003: Flyway migrations, baselined on init.sql

- **Status:** accepted, implemented.
- **Discussion:** issue #170 (roadmap rows C12 and S16).

## Context

The schema lived in `SQLRessources/init.sql`, a script that drops the `student_exchange_tools`
schema and recreates it with its reference data (options, programmes, countries, documents, one
denial reason). The PostgreSQL container ran it on its first start, the CI end-to-end job loaded
it with `psql`, and the integration tests loaded it into the embedded PostgreSQL. The application
did not know which schema a database had: a schema change was an edit of `init.sql`, applied by
hand (or by dropping the data), and nothing recorded which databases had it.

Databases created from `init.sql` exist (local ones, the Compose volume) and hold data that must
survive the move.

## Decision

### Flyway, run by the application at start-up

The backend depends on `spring-boot-starter-flyway` and `flyway-database-postgresql`, versions
managed by Spring Boot (Flyway 12.4 with Boot 4.1.2). Spring Boot's auto-configuration migrates
the database when the application starts, before Hibernate builds its `EntityManagerFactory`.
`application.properties`:

| Property | Value | Why |
|---|---|---|
| `spring.flyway.locations` | `classpath:db/migration` | the migrations live in `src/main/resources/db/migration` |
| `spring.flyway.schemas`, `spring.flyway.default-schema` | `student_exchange_tools` | the schema of the application (`hibernate.default_schema`); Flyway creates it when missing and keeps `flyway_schema_history` in it |
| `spring.flyway.baseline-on-migrate` | `true` | a database created from `init.sql` is adopted instead of refused |
| `spring.flyway.baseline-version` | `1` | that database is at version 1 |

Hibernate still never generates DDL (`ddl-auto=none`); the integration tests validate the entity
mappings against the migrated schema (`hbm2ddl.auto=validate`).

### The baseline: V1 is init.sql

`V1__baseline.sql` is `init.sql` without its first two statements: the tables, constraints and
reference data, statement for statement, and no account, partner or mobility. The schema itself is
created by Flyway (it is the schema of `spring.flyway.schemas`) before V1 runs. `init.sql` is
frozen at version 1 from now on:

- On an empty database, Flyway creates the schema and runs V1.
- On a database created from `init.sql` (the schema has tables but no history table), Flyway
  writes a baseline row at version 1 and runs nothing: the data is left as it is. A later migration
  then applies to it like to any other database.

`FlywayBaselineIT` guards the equivalence on the embedded PostgreSQL: V1 on an empty database and
`init.sql` on another must give the same columns (type, length, nullability, default), constraints
(and their definitions), indexes, sequences and rows; a database loaded from `init.sql` must be
baselined at version 1 with its row counts unchanged; a second start must find nothing to do.

### One migration per schema change, never edit a merged one

A schema change (a table, a column, a constraint, reference data the application needs) is a new
`V<n>__<description>.sql` in `src/main/resources/db/migration`, in the pull request that needs it,
with the entity change. A migration merged into `master` is never edited, renamed or deleted:
Flyway checks the checksums of the applied migrations and refuses to start on a mismatch, and a
database that already ran it would never see the edit. A mistake is corrected by the next
migration. `init.sql` and V1 are not edited either.

Demo data is not a migration: `SQLRessources/demo.sql` stays the demo data set, loaded by hand on a
migrated database.

### Where init.sql is still used

- The CI end-to-end job loads it before starting the jar, so that the e2e run goes through the
  baseline path (and starts from known data); `e2e/README.md` says the same for local runs.
- A developer can load it to reset a database to version 1; Flyway adopts it at the next start.
- `CountryFlagsTest` reads the country codes from it.

The PostgreSQL container of `docker-compose.yml` no longer mounts it: the database starts empty
and the backend creates the schema.

### Resetting a local database

Either drop the schema and restart the backend, which recreates it from the migrations:

```bash
psql -h localhost -U pguser01 -d testdb -c 'DROP SCHEMA student_exchange_tools CASCADE'
```

or load `init.sql` (it drops the schema, history table included, and recreates it at version 1)
and restart the backend. With Compose, `docker compose down -v` removes the database volume.
`flyway clean` is not used (Flyway disables it by default, and nothing turns it on).

## Consequences

- A schema change is reviewable (one file per change) and reaches every database the next time
  the backend starts; `flyway_schema_history` says which migrations a database has.
- The integration tests get their schema from the same migrations as the application
  (`RepositoryItConfig` runs Flyway, then `db/it-setup.sql`), so a migration that breaks an entity
  mapping fails the build.
- The full-application tests that run without a database (`ApplicationTests`,
  `OpenApiDocumentTest`, `ActuatorHealthWithoutDatabaseTests`) set `spring.flyway.enabled=false`:
  Flyway would otherwise fail their start-up.
- A database created from an older `init.sql` that lacks some reference rows (the countries added
  since, see `add-missing-countries.sql`) is still baselined at version 1: the baseline trusts the
  schema, it does not compare the data.
- Rolling back a migration means writing the next one; Flyway's undo migrations are not used.
