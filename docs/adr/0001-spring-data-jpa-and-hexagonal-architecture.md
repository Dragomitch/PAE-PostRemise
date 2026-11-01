# ADR 0001: Spring Data JPA persistence, organised as ports and adapters

- **Status:** accepted. Step 1 is implemented; steps 2 and 3 are planned.
- **Discussion:** issue #102 (the questions Q1–Q6 and the owner's answers), PR #91.

## Context

The persistence layer was made of 15 hand-written JDBC DAOs: about 85 `PreparedStatement`s, manual
`ResultSet` mapping, and every `SQLException` wrapped in a `FatalException`. PR #91 started moving
some of them to Spring Data JDBC and `JdbcClient`. Issue #102 settled the target:

| # | Question | Decision |
|---|----------|----------|
| Q1 | Architecture | Hexagonal (ports and adapters), following [Implementing Hexagonal Architecture in Java](https://medium.com/@emedinam/implementing-hexagonal-architecture-in-java-a-practical-guide-for-clean-domain-centric-design-37c7f8ca3e80) (E. M. Montenegro, 2025) "by the book". |
| Q2 | Persistence technology | Standard JPA (Spring Data JPA, Hibernate) everywhere: a full refactor rather than a mix of JDBC and JPA. |
| Q3 | Relations | Real JPA associations between the entities instead of foreign-key ids. |
| Q4 | Schema | Kept as it is (`SQLRessources/init.sql`). It changes only when that is justified and asked for. |
| Q5 | Spring Boot | 3.5 now (#134); Spring Boot 4 is a separate refactor. |
| Q6 | History | The JPA commits come on top of #91 with no history rewrite. #91's title and description are rewritten at the end. |

## Decision

### Hexagonal architecture, as the guide describes it

The guide's rules, as this project applies them:

- **The domain sits at the centre and depends on nothing technical.** Its model has no Spring or
  JPA annotation. Every dependency points inwards.
- **Ports are interfaces that the domain owns.**
  - *Driving (primary) ports* are the use cases the outside world calls.
  - *Driven (secondary) ports* are what the domain needs from the outside, such as storing and
    finding its objects. They are named in the domain's language, not after a technology.
- **The application layer implements the driving ports.** Its services orchestrate the domain
  model and the driven ports. Spring wires them by constructor injection.
- **Adapters implement or call the ports and only translate.**
  - Driving adapters: the REST controllers, with their own web DTOs.
  - Driven adapters: the JPA repositories, which map between the domain model and JPA entities.
  - Adapters hold no business rule.
- **Testing follows the same boundaries.** The domain and the use cases are unit-tested against
  mocked or in-memory ports. Each adapter is tested on its own: here, the `*IT` tests against
  PostgreSQL.

Naming, taken from the guide's examples:

| Role | Name | Example |
|------|------|---------|
| Driven port (domain) | `XxxRepository` | `PartnerRepository` (today's `PartnerDao`) |
| Spring Data interface (adapter) | `XxxJpaRepository` | `PartnerJpaRepository` |
| Driven adapter | `JpaXxxRepositoryAdapter` | `JpaPartnerRepositoryAdapter` (today's `PartnerPersistenceAdapter`) |
| JPA entity (adapter) | `XxxEntity` | `PartnerEntity` |
| Driving port (domain) | use-case interface | today's `PartnerUcc` |
| Application service | its implementation | today's `PartnerUccImpl` |

Target layout:

```
com.dragomitch.ipl.pae
├── domain
│   ├── model                    business objects and rules, no framework annotation
│   └── port
│       ├── in                   driving ports: the use cases
│       └── out                  driven ports: XxxRepository
├── application                  use-case services (@Service, @Transactional)
├── adapter
│   ├── in/web                   controllers, web DTOs, their mappers
│   └── out/persistence          JpaXxxRepositoryAdapter, PersistenceMapper
│       ├── entity               JPA entities (XxxEntity, composite keys)
│       └── repository           Spring Data interfaces (XxxJpaRepository)
└── config                       Spring configuration
```

### Steps

1. **JPA adapters behind the existing ports** (done, branch `claude/spring-data-jpa`). Every
   `XxxDao` is implemented by a JPA adapter in `adapter.out.persistence`. The JDBC DAOs and
   `DalBackendServices` are deleted. The DAO interfaces and the `*IT` tests are unchanged, apart
   from the contract changes listed below.
2. **Packaging and names.** Move the classes into the layout above. Rename `XxxDao` to
   `XxxRepository`, the adapters to `JpaXxxRepositoryAdapter`, and the use cases into `domain.port.in`
   and `application`.
3. **Pure domain model and web DTOs.** Separate the domain model from the DTO interfaces and
   `EntityFactory`: records for the web DTOs, mapped in `adapter.in.web`. Use typed filters (enums
   instead of strings) and `Optional` results in the driven ports.

Spring Boot 4 comes after these steps, in its own PR.

### Persistence rules

- **The schema is owned by `SQLRessources/init.sql`.**
  - The application never generates DDL (`spring.jpa.hibernate.ddl-auto=none`).
  - The integration tests validate the mapping against the real schema (`hbm2ddl.auto=validate`).
  - `hibernate.default_schema` is `student_exchange_tools`.
- **Entities live in `adapter.out.persistence.entity`.**
  - They use field access and map every association as `LAZY`.
  - `CHAR` columns use `@JdbcTypeCode(SqlTypes.CHAR)`.
  - Reference data (options, programmes, countries, documents) is `@Immutable`.
- **Every read runs one SQL statement.** It fetches what its DTOs carry with an `@EntityGraph`
  or a `JOIN FETCH`. `QueryCountIT` enforces this (an N+1 guard based on Hibernate statistics),
  and `spring.jpa.open-in-view=false`.
- **Optimistic locking.**
  - Versioned tables use `@Version`.
  - An update loads the entity, compares its version with the DTO's, copies the fields and
    flushes.
  - Every update increments the version, even when it changes nothing, as the JDBC DAOs did
    (`Versions.flushIncrementingVersion`, pinned by `VersionContractIT`). JPA alone would leave an
    unchanged entity's version as it is. The use cases rely on the increment: a partner edit takes
    the stored partner version and detects a stale form through its address update.
  - An unknown id or a stale version throws `ConcurrentModificationException`, answered with a
    409 `CONCURRENT_MODIFICATION`.
  - The two updates whose contract increments the version unconditionally are bulk JPQL updates:
    `UserDao.promoteToProfessor` and `MobilityDocumentDao.fillInDocument`.
- **Keys.**
  - Composite keys are `@EmbeddedId`s, and `@MapsId` shares a key with the parent: a mobility
    with its choice, a nominated student with their user.
  - A create with an assigned key flushes at once, so that a constraint violation surfaces inside
    the call.
  - A mobility document is linked by an HQL `INSERT`, because Hibernate takes a new entity with
    an assigned composite key and a version for a detached one.
- **Exceptions are translated.** The adapters are `@Repository` beans, so Spring turns the JPA
  and SQL exceptions into its `DataAccessException` hierarchy.

### Contract changes from the JDBC DAOs

These are visible to the callers. The `*IT` tests and the port javadoc assert them:

1. **SQL failures are `DataAccessException`s instead of `FatalException`s.** A constraint
   violation (duplicate key, unknown foreign key) is a `DataIntegrityViolationException`, which
   `ApiExceptionHandler` answers with a 409 `DATA_CONFLICT` instead of a 500.
2. **An unknown filter or lookup column is an `InvalidDataAccessApiUsageException`** wrapping the
   `IllegalArgumentException`: Spring translates an `IllegalArgumentException` thrown by a
   `@Repository`. This concerns `PartnerDao.findAll`, `MobilityChoiceDao.findAll` and
   `UserDao.findBy`. It is still a programming error, because the API rejects an unknown filter
   first.
3. **Lists are ordered by id.** Some JDBC queries had no `ORDER BY`.

The submission time of a new mobility choice is still the database's time, read in the create.

## Consequences

- No more hand-written SQL plumbing. Queries are derived queries, entity graphs or JPQL built
  from constant fragments, with no user input concatenated into them.
- The DAO interfaces still use the DTO interfaces, so the domain still depends on the shape of
  the API. Step 3 removes that.
- The tests are unchanged in shape:
  - the use-case unit tests run against the in-memory mock DAOs;
  - the `*IT` tests (embedded PostgreSQL) are the contract of the driven adapters;
  - the persistence coverage gate (JaCoCo) now guards `adapter.out.persistence`.
