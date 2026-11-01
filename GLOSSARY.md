# Glossary

The words of the domain, and of the architecture (see
[ADR 0001](docs/adr/0001-spring-data-jpa-and-hexagonal-architecture.md)), as the code uses them.
The UI and the stored values are in French. The code is in English.

## Domain

| Term | Meaning | In the code |
|------|---------|-------------|
| **Student** / **Professor** | The two roles of a user. A professor manages the mobilities. A user signs up as a student and can be promoted to professor. | `UserDto.ROLE_STUDENT` (`Student`), `ROLE_PROFESSOR` (`Professor`) |
| **Option** | The study option (department) of a student, identified by a three-letter code. | `OptionDto`, table `options` |
| **Programme** | The exchange programme of a mobility: Erasmus+, Erabel, FAME. Each country belongs to one. | `ProgrammeDto`, table `programmes` |
| **Partner** | A host institution or company abroad. *Official* partners are visible to the students. A partner welcomes some options, can be *archived*, and cannot be archived while it has active choices. | `PartnerDto`, tables `partners`, `partner_options` |
| **Mobility choice** | A wish a student submits: type, academic year, term, programme, country and, optionally, partner, in their order of preference. A professor can *deny* it (with a denial reason); the student can *cancel* it (with a cancellation reason). | `MobilityChoiceDto`, table `mobility_choices` |
| **Mobility type** | `SMS`: studies; `SMP`: placement. | `MobilityChoiceDto.MOBILITY_TYPE_*` |
| **Active choice** | A choice of the current academic year that is neither denied nor cancelled and did not become a mobility. | `MobilityChoiceDao.FILTER_ACTIVE_MOBILITIES_CHOICES` |
| **Mobility** | A choice accepted by a professor, followed until it is closed. It shares its id with its choice. | `MobilityDto`, table `mobilities` |
| **Mobility state** | `Créée` → `En préparation` → `A payer` → `En cours` → `Solde à payer` → `Terminée`, or `Annulée` (the state before the cancellation is kept). | `MobilityDto.STATE_*` |
| **Nominated student** | The personal and bank data a student gives once a mobility is granted. It shares its id and version with the user. | `NominatedStudentDto`, table `nominated_students` |
| **Document** | A document a programme requires: category `D` before departure, `R` after return. | `DocumentDto`, table `documents` |
| **Mobility document** | The link between a mobility and a required document, with its *filled in* flag. | `MobilityDocumentDao`, table `mobility_documents` |
| **Payment** | A requested payment of a mobility's grant: `D` for the first request, `R` for the second one (the balance). It is read from the request dates of the mobility. | `PaymentDto`, `PaymentDao` |
| **Denial reason** | A professor's reason for refusing a choice or a mobility, picked from a list. | `DenialReasonDto`, table `denial_reasons` |
| **ProEco / second software** | The external software the mobility must be encoded in; each has its own flag on the mobility. | `MobilityDto.isEncodedInProEco()`, `isEncodedInSecondSoftware()` |

## Architecture

| Term | Meaning | In the code |
|------|---------|-------------|
| **Domain** | The business objects and rules, independent of any framework. | `business` today, `domain.model` in the target layout |
| **Driving (primary) port** | A use case the outside world calls. | `XxxUcc` today, `domain.port.in` in the target layout |
| **Driven (secondary) port** | What the domain needs from the outside, such as storing and finding its objects. | `XxxDao` today, `XxxRepository` in `domain.port.out` in the target layout |
| **Application service** | The implementation of a driving port: orchestrates the domain and the driven ports in one transaction. | `XxxUccImpl` (`@Service`, `@Transactional`) |
| **Driving adapter** | Calls a driving port from a technology: the REST controllers. | `web` today, `adapter.in.web` in the target layout |
| **Driven adapter** | Implements a driven port with a technology: the JPA persistence adapters. | `adapter.out.persistence.XxxPersistenceAdapter` |
| **JPA entity** | The persistence representation of a table, private to the persistence adapter. | `adapter.out.persistence.entity.XxxEntity` |
| **Spring Data repository** | The Spring Data interface a driven adapter uses for its queries. | `adapter.out.persistence.repository.XxxJpaRepository` |
| **DTO** | The data a use case receives and returns, today also the JSON of the API. | `business.dto.XxxDto` |
| **Optimistic locking** | An update succeeds only if the row still has the version that was read. Otherwise the API answers 409 `CONCURRENT_MODIFICATION`. | `@Version`, `ConcurrentModificationException` |
