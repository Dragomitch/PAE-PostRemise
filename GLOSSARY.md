# Glossary

The words of the domain, and of the architecture (see
[ADR 0001](docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md)), as the code uses them.
The UI and the stored values are in French. The code is in English.

## Domain

| Term | Meaning | In the code |
|------|---------|-------------|
| **Student** / **Professor** | The two roles of a user. A professor manages the mobilities. A user signs up as a student and can be promoted to professor. | `Role.STUDENT` (stored and sent as `Student`), `Role.PROFESSOR` (`Professor`) |
| **Option** | The study option (department) of a student, identified by a three-letter code. | `Option`, table `options` |
| **Programme** | The exchange programme of a mobility: Erasmus+, Erabel, FAME. Each country belongs to one. | `Programme`, table `programmes` |
| **Partner** | A host institution or company abroad. *Official* partners are visible to the students. A partner welcomes some options, can be *archived*, and cannot be archived while it has active choices. | `Partner`, `PartnerOption`, tables `partners`, `partner_options` |
| **Mobility choice** | A wish a student submits: type, academic year, term, programme, country and, optionally, partner, in their order of preference. A professor can *deny* it (with a denial reason); the student can *cancel* it (with a cancellation reason). | `MobilityChoice`, table `mobility_choices` |
| **Mobility type** | `SMS`: studies; `SMP`: placement. | `MobilityType.SMS`, `MobilityType.SMP` |
| **Active choice** | A choice of the current academic year that is neither denied nor cancelled and did not become a mobility. | `MobilityChoiceFilter.ACTIVE` |
| **Mobility** | A choice accepted by a professor, followed until it is closed. It shares its id with its choice. | `Mobility`, table `mobilities` |
| **Mobility state** | `Créée` → `En préparation` → `A payer` → `En cours` → `Solde à payer` → `Terminée`, or `Annulée` (the state before the cancellation is kept). | `MobilityState` (the French label is the stored and sent value; `nextStates()`, `canMoveTo()`, `Mobility.moveTo()`) |
| **Nominated student** | The personal and bank data a student gives once a mobility is granted. It shares its id and version with the user. | `NominatedStudent`, table `nominated_students` |
| **Document** | A document a programme requires: category `D` before departure, `R` after return. | `Document`, `DocumentCategory.DEPARTURE` (`D`), `DocumentCategory.RETURN` (`R`), table `documents` |
| **Mobility document** | The link between a mobility and a required document, with its *filled in* flag. | `MobilityDocumentRepository`, table `mobility_documents` |
| **Payment** | A requested payment of a mobility's grant: `D` for the first request, `R` for the second one (the balance). It is read from the request dates of the mobility. | `Payment`, `PaymentRepository` |
| **Denial reason** | A professor's reason for refusing a choice or a mobility, picked from a list. | `DenialReason`, table `denial_reasons` |
| **Mobility choice filter** | The list of choices a professor asks for: all, active (the default), canceled, rejected, passed (previous years). | `MobilityChoiceFilter` (query parameter `filter`) |
| **Document filter** | The documents a CSV export of a mobility keeps: departure (`D`), filled-in departure (`DF`), return (`R`), filled-in return (`RF`); every document without a filter. | `DocumentFilter` |
| **Partner filter** | The list of partners: every partner visible to the user, the archived ones whose name contains a value, or those of a country. | `PartnerFilter` (query parameters `filter`, `value`) |
| **ProEco / second software** | The external software the mobility must be encoded in; each has its own flag on the mobility. | `Mobility.isEncodedInProEco()`, `isEncodedInSecondSoftware()` |

## Architecture

| Term | Meaning | In the code |
|------|---------|-------------|
| **Domain** | The business objects and rules, independent of any framework: plain classes and enums, depending on the JDK only (`ArchitectureTest`). | `domain.model` |
| **Driving (primary) port** | A use case the outside world calls. | `domain.port.in.XxxService` |
| **Driven (secondary) port** | What the domain needs from the outside, such as storing and finding its objects. | `domain.port.out.XxxRepository` (a single result is an `Optional`), `domain.port.out.PasswordHasher` |
| **Application service** | The implementation of a driving port: orchestrates the domain and the driven ports in one transaction. | `application.XxxServiceImpl` (`@Service`, `@Transactional`) |
| **Driving adapter** | Calls a driving port from a technology: the REST controllers. | `adapter.in.web` |
| **Driven adapter** | Implements a driven port with a technology: the JPA persistence adapters, the BCrypt password hasher. | `adapter.out.persistence.JpaXxxRepositoryAdapter`, `adapter.out.security.BCryptPasswordHasher` |
| **JPA entity** | The persistence representation of a table, private to the persistence adapter. | `adapter.out.persistence.entity.XxxEntity` |
| **Spring Data repository** | The Spring Data interface a driven adapter uses for its queries. | `adapter.out.persistence.repository.XxxJpaRepository` |
| **Web DTO** | The JSON of the API: a record with the Bean Validation constraints, the Jackson and the OpenAPI annotations. The use cases never see it. | `adapter.in.web.dto.XxxDto` |
| **Web mapper** | Converts the web DTOs to and from the domain model, the labels to and from the enums. | `adapter.in.web.mapping.XxxMapper` |
| **Optimistic locking** | An update succeeds only if the row still has the version that was read. Otherwise the API answers 409 `CONCURRENT_MODIFICATION`. | `@Version`, `ConcurrentModificationException` |
