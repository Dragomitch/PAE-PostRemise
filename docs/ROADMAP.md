# Engineering roadmap

Proposals following the Spring migration (PRs #86–#92). Requirement 7 (workflow and CI/CD) and
requirement 8 (making better use of Spring and Spring Boot) are listed below. Each item says what
it replaces and why.

Proposals for the next steps. Status legend: ✅ done in the current PR batch · 🚧 started · 💡 proposal.

## 1. Workflow and CI/CD (requirement 7)

| # | Proposal | Why | Status |
|---|---|---|---|
| C1 | Single `CI` workflow on every PR: backend, frontend, Docker build, workflow lint, test report, `CI passed` gate | One required check, readable reports | ✅ #87 |
| C2 | Branch protection on `master`: require `CI passed` and one review, require up-to-date branches, block force pushes | CI means nothing if red PRs can be merged | 💡 settings |
| C3 | Enable **"Automatically delete head branches"** + prefer merge queue | Stacked PRs then retarget to `master` automatically (the #81–#84 incident) | 💡 settings |
| C4 | Dependabot for `maven`, `npm` (frontend) and `github-actions`, grouped weekly (minor/patch grouped, majors separate) | 46 npm vulnerabilities were found; today's bumps arrive one by one and conflict | 💡 |
| C5 | CodeQL (Java + TypeScript) and `dependency-review-action` on PRs | Catch vulnerable dependencies/injection before merge | 💡 |
| C6 | Pin third-party actions to commit SHAs (Dependabot keeps them fresh) | Supply-chain hardening | 💡 |
| C7 | Coverage ratchet: JaCoCo `check` + Karma thresholds fail the build; PR comment shows the delta | Coverage can only go up | ✅ #89 (JaCoCo), #88 (Karma), raised in #92 |
| C8 | Separate unit (Surefire) and integration (Failsafe, embedded PostgreSQL) suites, both reported | Fast feedback + real SQL checked | ✅ #89 |
| C9 | Playwright end-to-end smoke job (jar + PostgreSQL service container + legacy UI and Angular) on PRs touching `src/main/webapp`, `frontend` or controllers | The UI regressions (#20/#22) were only caught manually | 💡 |
| C10 | Release workflow on tags: build once, push versioned images to GHCR (`backend`, `frontend`), SBOM + provenance (`docker/build-push-action` attestations), GitHub Release with changelog | Reproducible deployable artifacts | 💡 |
| C11 | Deploy workflow with GitHub Environments (`staging` auto on `master`, `production` with manual approval), secrets (`JWT_SECRET`, DB) per environment | No secrets in the repo, audited promotions | 💡 |
| C12 | Database migrations with Flyway (baseline = current `init.sql`), run by the app at start-up and checked in CI | Schema changes become reviewable and repeatable | 💡 |
| C13 | Conventional Commits check (PR title) + Release Please / semantic-release for versions and changelog | The repo already uses `type: subject`; automate versioning | 💡 |
| C14 | Pre-commit hooks / formatting in CI: Spotless (google-java-format, as backend/AGENTS.md asks) + Prettier/ESLint for Angular | Style debates out of reviews | 💡 |
| C15 | `CODEOWNERS` + PR template (summary, test plan, screenshots for UI) | Consistent reviews | 💡 |
| C16 | Nightly workflow: full suite against the latest Spring Boot patch + `npm audit`/OWASP dependency-check, opening an issue on failure | Early warning, without slowing PRs | 💡 |

## 2. Using Spring / Spring Boot better (requirement 8)

| # | Improvement | Today | With Spring | Status |
|---|---|---|---|---|
| S1 | Spring MVC `@RestController`s | Custom `RoutingServlet` + `@Route`/`Invoker` reflection | Standard mappings, content negotiation, OpenAPI-able | ✅ #90 |
| S2 | Spring Security for authn/authz | `SessionManager` + `@Role` checked by hand | Resource-server JWT from cookie, `@PreAuthorize`, CSRF, stateless | ✅ #90 |
| S3 | `@Transactional` | `UnitOfWork` thread-local semaphore + reflection on `@DaoClass` | Declarative transactions, rollback rules | ✅ #90 |
| S4 | RFC 9457 `ProblemDetail`/`ErrorResponse` + `@RestControllerAdvice` | `errors.json` + `ErrorFormat` + numeric codes | Standard error body, `MessageSource` i18n | ✅ #92 (+ frontend #88) |
| S5 | `MessageSource` + `LocaleResolver` (Accept-Language) | French strings hard-coded | `messages_{fr,en}.properties` | ✅ #92 (+ `@angular/localize` in #88) |
| S6 | Bean Validation (`@Valid`, `@Validated`, constraints on DTOs, method validation on services) | 171 manual `checkX`/`isAValidX` calls + 8 `checkDataIntegrity` methods | Declarative, reusable, automatically reported as problems | ✅ #92 |
| S7 | Spring Data JDBC repositories | 15 hand-written DAOs, 85 `PreparedStatement`s, 54 `catch (SQLException)` | Derived queries, `@Query`, paging, `@Version` optimistic locking | 🚧 #91 (7 of 15 DAOs; plan for the rest in `backend/AGENTS.md`) |
| S8 | `JdbcClient`/`JdbcTemplate` for the queries that stay hand-written | Manual `ResultSet` mapping and resource handling | `RowMapper`s, exception translation (`DataAccessException`) | ✅ #91 (the 7 join-heavy DAOs) |
| S9 | `@Version` instead of hand-managed `version` columns | Versions bumped manually in services/UnitOfWork | Optimistic locking by the framework → 409 problem | 🚧 #91 (Address, User) |
| S10 | Replace DTO interface + impl pairs (and `EntityFactory`) with Java `record`s (API DTOs) and entity classes | 13 interfaces ×2 + a factory + Jackson abstract-type mappings | Plain types; Jackson and Spring Data bind them directly | 💡 |
| S11 | Mapping layer with MapStruct (entity ↔ DTO) | Manual copying in services/DAOs | Compile-time generated mappers | 💡 |
| S12 | Enums instead of string constants for roles and states (`Role`, `MobilityState` with `@JsonValue`/converters) | 63 string comparisons of roles, French state labels as data | Type-safe, translatable labels | 💡 |
| S13 | Typed responses instead of `Map<String, Object>` (18 endpoints wrap lists in `{"data": ...}`) | Untyped maps | Records such as `Page<T>`/`ListResponse<T>` | 💡 |
| S14 | Pagination and sorting with Spring Data `Pageable` on list endpoints | Full tables returned to DataTables | `?page=&size=&sort=` | 💡 |
| S15 | `@ConfigurationProperties` records (`app.jwt.*`, `app.cors.*`, `app.cookie.*`) with validation | Scattered `@Value` | Type-safe, documented (`spring-boot-configuration-processor`) | 🚧 `app.session.*` in #90 |
| S16 | Flyway via Spring Boot auto-configuration | Manual `init.sql` | Versioned migrations (see C12) | 💡 |
| S17 | Spring Boot Actuator (health incl. DB, info, metrics, Prometheus) | No health endpoint | Docker/Compose health checks, monitoring | 💡 |
| S18 | Structured logging (Boot 3.4 `logging.structured.format.console=ecs`) and request correlation IDs (Micrometer Tracing) | Plain text logs | Searchable logs | 💡 |
| S19 | springdoc-openapi: generated OpenAPI 3 + Swagger UI; generate the Angular client from it | API contract only in code | Typed frontend client, contract tests | 💡 |
| S20 | Jackson CSV (`jackson-dataformat-csv`) or an `HttpMessageConverter` for the exports | `CsvStringBuilder`/`CsvEncoderServerSide` | Declarative CSV columns | 💡 |
| S21 | `Clock` bean injected instead of `LocalDateTime.now()` (8 call sites) | Time-dependent logic hard to test | Deterministic tests | 💡 |
| S22 | Testcontainers with `@ServiceConnection` (Boot 3.1+) for integration tests in CI | Embedded PostgreSQL (fine locally) | Same engine/version as production, zero config | 💡 |
| S23 | Test slices: `@WebMvcTest`, `@DataJdbcTest`, `@JsonTest` instead of full contexts | Mostly full-context/unit | Faster, focused tests | 🚧 `@WebMvcTest` in #90; `@DataJdbcTest` next |
| S24 | Upgrade Spring Boot 3.2 → 3.5 (3.2 is out of OSS support) with the properties migrator | Unsupported line | Security fixes, structured logging, `RestClient` improvements | 💡 |
| S25 | Virtual threads (`spring.threads.virtual.enabled=true`, Java 21) | Platform threads | Cheap concurrency for blocking JDBC | 💡 |
| S26 | Layered jar + `spring-boot:build-image` (buildpacks) or `jarmode=tools extract` in the Dockerfile | Fat jar copied as one layer | Smaller, cache-friendly images | 💡 |
| S27 | Graceful shutdown + readiness/liveness probes (`server.shutdown=graceful`, Actuator probes) | Hard stop | Zero-downtime deploys | 💡 |
| S28 | Caching reference data (`@Cacheable` on countries, options, programmes) | Queried on every page | Fewer DB round-trips | 💡 |
| S29 | Serve the Angular build from the backend or retire the legacy jQuery UI once the Angular app covers its screens | Two UIs to maintain; legacy one is French-only | One i18n'd UI | 💡 |
| S30 | Spring Modulith (or ArchUnit) tests to enforce the layering web → service → persistence | Layering by convention | Build fails on violations | 💡 |

## 3. Behaviour decisions to take

The new tests pin these behaviours without changing them, because changing them is a product
decision. Each one is documented next to the test that pins it.

| # | Behaviour | Where |
|---|---|---|
| D1 | A partner with no option is invisible to every partner query (inner join on `partner_options`). | `PartnerDaoImpl` |
| D2 | A mobility whose choice has no partner is invisible to every mobility query. | `MobilityDaoImpl` |
| D3 | `confirmPayment` accepts the first payment whatever the mobility state, not only "A payer"; the first filled-in document always moves a mobility to "En préparation". | `MobilityUccImpl` |
| D4 | `confirmDocument` does not refuse a cancelled or closed mobility. | `MobilityUccImpl` |
| D5 | `POST /partners/{id}` (add an option) has no ownership check: any authenticated user can add options to any partner. | `PartnerController` / `PartnerUccImpl` |
| D6 | `PartnerUcc.edit` answers 500 when the option list contains a `null` element (add `List<@NotNull …>`). | `PartnerDto` |
| D7 | `DenialReasonDao.update` has no optimistic locking; `NominatedStudent.create` stores the DTO's version but returns 1. | DAO ITs |
| D8 | The demo IBAN in `SQLRessources/demo.sql` fails the MOD-97 check introduced by `@Iban`. | `demo.sql` |
| D9 | The `countries` table uses retired codes (CS, AN) and lacks RS, ME, SS, CW, SX, BQ, BL, GG, JE (flags already exist). | `init.sql` |
| D10 | Legacy UI (`src/main/webapp`) is French-only; only server messages are localized. Porting its screens to the Angular app (S29) removes the need to translate it. | `app.js` |
