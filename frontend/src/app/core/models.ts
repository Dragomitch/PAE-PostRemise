/**
 * The payloads of the REST API under the names the screens use, derived from the schema types
 * generated from the OpenAPI document (`core/api/`, `npm run api:generate`).
 *
 * Every member is picked by name from a generated type, so a property renamed or removed in the
 * backend fails the build here. What the document cannot say is adjusted, member by member:
 *
 * - presence: the document marks as required what Bean Validation requires of a request body and
 *   leaves everything else optional, while the JSON always carries the primitive members (ids,
 *   versions, flags, term...) and leaves out null values. `Ref` makes required the members the
 *   screens can rely on;
 * - nested objects: a mobility choice's `user`, `programme` or `partner` is a reference (id and
 *   a few names), not the whole object the document describes, and requests send references by id
 *   or code;
 * - the members the JSON never has: `version` of options and programmes (`@JsonIgnore`);
 * - value sets the document only knows as strings (`MobilityType`, the filters).
 *
 * Never edit the generated files; see frontend/AGENTS.md ("Generated API client").
 */
import type {
  AddressDto,
  CountryDto,
  DenialReasonDto,
  DocumentDto,
  MobilityChoiceDto,
  MobilityDto,
  NominatedStudentDto,
  OptionDto,
  PartnerDto,
  PartnerOptionDto,
  ProgrammeDto,
  SignInRequest,
  UserDto,
  UserDtoWritable,
} from './api';

/**
 * The members `K` of the generated type `T`, required, and its members `O`, optional: the subset
 * of a payload that a screen sends or reads.
 */
type Ref<T, K extends keyof T, O extends keyof T = never> = {
  [P in K]-?: Exclude<T[P], undefined>;
} & { [P in O]?: T[P] };

/** Body of `POST /api/1.0/session`. */
export type Credentials = SignInRequest;

/** An option, by its code (its name when the API reads it; never its version). */
export type OptionRef = Ref<OptionDto, 'code', 'name'>;

/** Body of `POST /api/1.0/users` (sent as is, not wrapped). */
export type SignUpRequest = Ref<
  UserDtoWritable,
  'username' | 'password' | 'firstName' | 'lastName' | 'email'
> & { option: OptionRef };

/** Roles returned by the backend in `User.role` (`UserDto.ROLE_PROFESSOR` / `ROLE_STUDENT`). */
export const ROLE_PROFESSOR = 'Professor';
export const ROLE_STUDENT = 'Student';

/**
 * User as returned by the session and users endpoints. `registrationDate` is
 * `yyyy-MM-ddTHH:mm:ss[.SSSSSS]`, local time of the server.
 */
export type User = Ref<
  UserDto,
  'username',
  'id' | 'firstName' | 'lastName' | 'email' | 'role' | 'registrationDate' | 'version'
> & { option?: OptionRef };

/** Country of a partner's address, identified by its ISO 3166-1 alpha-2 code. */
export type Country = Ref<CountryDto, 'countryCode' | 'name'>;

/** An option offered by a partner, in one of its departments. */
export type PartnerOption = Ref<PartnerOptionDto, 'code', 'name' | 'departement'>;

/** Partner as listed by `GET /api/1.0/partners` (only the members the screens read). */
export type Partner = Ref<PartnerDto, 'id' | 'fullName' | 'official' | 'archived'> & {
  address: { country: Country };
  options: PartnerOption[];
};

/** A country referenced by its code (nationality, country of an address), with its name when read. */
export type CountryRef = Ref<CountryDto, 'countryCode', 'name'>;

/** A postal address; the region may be empty (`null`) but is always sent. */
export type Address = Ref<
  AddressDto,
  'street' | 'number' | 'city' | 'postalCode',
  'id' | 'version'
> & {
  region: AddressDto['region'] | null;
  country: CountryRef;
};

/**
 * The personal data of a nominated student, on top of their user account (whose names, e-mail,
 * username and option are part of the same object). The id is the user's.
 *
 * `title`: `M` (Madame), `Mr` (Monsieur) or `Ms` (Mademoiselle) in the legacy form, the API also
 * accepts `Mrs`; `birthdate`: `yyyy-MM-dd`; `gender`: `M`, `F` or `O`; `nbrPassedYears`: years
 * already passed in higher education, at least 1; `cardHolder`: the holder of the bank account,
 * the API stores the student's name when it is empty.
 */
export type NominatedStudent = User &
  Ref<
    NominatedStudentDto,
    | 'id'
    | 'firstName'
    | 'lastName'
    | 'email'
    | 'title'
    | 'birthdate'
    | 'phoneNumber'
    | 'gender'
    | 'nbrPassedYears'
    | 'iban'
    | 'bankName'
    | 'bic'
  > & {
    nationality: CountryRef;
    address: Address;
    cardHolder?: NominatedStudentDto['cardHolder'] | null;
  };

/** A mobility programme: Erasmus+, Erabel, FAME (never its version). */
export type Programme = Ref<ProgrammeDto, 'id' | 'programmeName'>;

/** A country of `GET /api/1.0/countries`, with the programme its mobilities belong to. */
export type ProgrammeCountry = Country & { programme: Ref<ProgrammeDto, 'id'> };

/** A reason a professor gives when rejecting a mobility choice. */
export type DenialReason = Ref<DenialReasonDto, 'id' | 'reason'>;

/** Studies abroad (`SMS`) or internship abroad (`SMP`); the document only has a pattern. */
export type MobilityType = 'SMS' | 'SMP';

/**
 * Filters of `GET /api/1.0/mobilityChoices` for a professor (`MobilityChoiceService.FILTER_REGEXP`):
 * `active` (the default of the API) is the current year's pending choices.
 */
export type MobilityChoiceFilter = 'active' | 'all' | 'canceled' | 'rejected' | 'passed';

/**
 * A mobility choice as listed by `GET /api/1.0/mobilityChoices` (only the members the screen reads).
 * `term` is 1 (departure in September) or 2 (departure in February).
 */
export type MobilityChoice = Ref<
  MobilityChoiceDto,
  'id' | 'preferenceOrder' | 'academicYear' | 'term',
  'submissionDate'
> & {
  mobilityType: MobilityType;
  user: Ref<UserDto, 'id', 'firstName' | 'lastName'>;
  programme: Ref<ProgrammeDto, 'id', 'programmeName'>;
  country?: CountryRef | null;
  partner?: Ref<PartnerDto, 'id', 'fullName'> | null;
  denialReason?: Ref<DenialReasonDto, 'id', 'reason'> | null;
  cancellationReason?: MobilityChoiceDto['cancellationReason'] | null;
};

/** Body of `POST /api/1.0/mobilityChoice`: the references are sent by id (or country code). */
export type MobilityChoiceRequest = Ref<
  MobilityChoiceDto,
  'preferenceOrder' | 'academicYear' | 'term'
> & {
  user: Ref<UserDto, 'id'>;
  mobilityType: MobilityType;
  programme: Ref<ProgrammeDto, 'id'>;
  country?: Ref<CountryDto, 'countryCode'>;
  partner?: Ref<PartnerDto, 'id'>;
};

/** States of a mobility, as the API spells them (the enum of `MobilityDto.state`). */
export const MOBILITY_STATES = {
  created: 'Créée',
  inPreparation: 'En préparation',
  toBePaid: 'A payer',
  inProgress: 'En cours',
  balanceToBePaid: 'Solde à payer',
  closed: 'Terminée',
  cancelled: 'Annulée',
} as const satisfies Record<string, NonNullable<MobilityDto['state']>>;

/** A document of a mobility: `D` departure document, `R` return document. */
export type MobilityDocument = Ref<DocumentDto, 'id' | 'name' | 'filledIn'> & {
  category: 'D' | 'R';
};

/**
 * A mobility: a confirmed mobility choice, with the same id. The list of
 * `GET /api/1.0/mobilities` leaves out the documents and most of the student's data, which the
 * detail (`GET /mobilities/{id}`) carries. Dates are `yyyy-MM-ddTHH:mm:ss[.SSSSSS]`, local time of
 * the server (except `submissionDate`, `yyyy-MM-dd HH:mm:ss`, which the screens do not read).
 */
export type Mobility = Ref<
  MobilityDto,
  | 'id'
  | 'version'
  | 'academicYear'
  | 'term'
  | 'state'
  | 'encodedInProEco'
  | 'encodedInSecondSoftware'
> & {
  mobilityType: MobilityType;
  stateBeforeCancellation?: MobilityDto['stateBeforeCancellation'] | null;
  programme: Ref<ProgrammeDto, 'id', 'programmeName' | 'externalSoftName'>;
  country?: CountryRef | null;
  partner?: Ref<PartnerDto, 'id', 'fullName'> | null;
  nominatedStudent: Partial<NominatedStudent> & { id: number };
  professorInCharge?: Ref<UserDto, never, 'id' | 'firstName' | 'lastName'> | null;
  documents?: MobilityDocument[];
  firstPaymentRequestDate?: MobilityDto['firstPaymentRequestDate'] | null;
  secondPaymentRequestDate?: MobilityDto['secondPaymentRequestDate'] | null;
  denialReason?: Ref<DenialReasonDto, 'id', 'reason'> | null;
  cancellationReason?: MobilityDto['cancellationReason'] | null;
};

/**
 * Filters of the CSV export of a mobility's documents (`MobilityService.DOCUMENT_FILTER_REGEXP`):
 * departure (`D`) or return (`R`) documents, `F` keeping the filled-in ones; none exports them all.
 */
export type MobilityDocumentFilter = 'D' | 'DF' | 'R' | 'RF';
