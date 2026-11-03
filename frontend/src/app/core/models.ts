export interface Credentials {
  username: string;
  password: string;
}

export interface OptionRef {
  code: string;
  /** Sent by the API when it reads an option, never needed in a request. */
  name?: string;
}

/** Body of `POST /api/1.0/users` (sent as is, not wrapped). */
export interface SignUpRequest {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email: string;
  option: OptionRef;
}

/** Roles returned by the backend in `User.role` (`UserDto.ROLE_PROFESSOR` / `ROLE_STUDENT`). */
export const ROLE_PROFESSOR = 'Professor';
export const ROLE_STUDENT = 'Student';

/** User as returned by the session and users endpoints. */
export interface User {
  id?: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
  option?: OptionRef;
  /** `yyyy-MM-ddTHH:mm:ss[.SSSSSS]`, local time of the server. */
  registrationDate?: string;
  version?: number;
}

/** Country of a partner's address (`CountryDto`), identified by its ISO 3166-1 alpha-2 code. */
export interface Country {
  countryCode: string;
  name: string;
}

/** An option offered by a partner, in one of its departments (`PartnerOptionDto`). */
export interface PartnerOption {
  code: string;
  name?: string;
  departement?: string;
}

/** Partner as listed by `GET /api/1.0/partners` (only the members the screens read). */
export interface Partner {
  id: number;
  fullName: string;
  address: { country: Country };
  options: PartnerOption[];
  official: boolean;
  archived: boolean;
}

/** A country referenced by its code (nationality, country of an address), with its name when read. */
export interface CountryRef {
  countryCode: string;
  name?: string;
}

/** A postal address (`AddressDto`); the region may be empty but is always sent. */
export interface Address {
  id?: number;
  version?: number;
  street: string;
  number: string;
  city: string;
  postalCode: string;
  region: string | null;
  country: CountryRef;
}

/**
 * The personal data of a nominated student (`NominatedStudentDto`), on top of their user account
 * (whose names, e-mail, username and option are part of the same object). The id is the user's.
 */
export interface NominatedStudent extends User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  /** `M` (Madame), `Mr` (Monsieur) or `Ms` (Mademoiselle) in the legacy form; the API also accepts `Mrs`. */
  title: string;
  /** `yyyy-MM-dd`. */
  birthdate: string;
  nationality: CountryRef;
  address: Address;
  phoneNumber: string;
  /** `M`, `F` or `O`. */
  gender: string;
  /** Years already passed in higher education, at least 1. */
  nbrPassedYears: number;
  iban: string;
  bankName: string;
  bic: string;
  /** The holder of the bank account; the API stores the student's name when it is empty. */
  cardHolder?: string | null;
}

/** Collection responses are wrapped in `{ "data": [...] }`. */
export interface DataResponse<T> {
  data: T[];
}

/** A mobility programme (`ProgrammeDto`): Erasmus+, Erabel, FAME. */
export interface Programme {
  id: number;
  programmeName: string;
}

/** A country of `GET /api/1.0/countries`, with the programme its mobilities belong to. */
export interface ProgrammeCountry extends Country {
  programme: { id: number };
}

/** A reason a professor gives when rejecting a mobility choice (`DenialReasonDto`). */
export interface DenialReason {
  id: number;
  reason: string;
}

/** Studies abroad (`SMS`) or internship abroad (`SMP`), see `MobilityChoiceDto`. */
export type MobilityType = 'SMS' | 'SMP';

/**
 * Filters of `GET /api/1.0/mobilityChoices` for a professor (`MobilityChoiceService.FILTER_REGEXP`):
 * `active` (the default of the API) is the current year's pending choices.
 */
export type MobilityChoiceFilter = 'active' | 'all' | 'canceled' | 'rejected' | 'passed';

/** A mobility choice as listed by `GET /api/1.0/mobilityChoices` (only the members the screen reads). */
export interface MobilityChoice {
  id: number;
  user: { id: number; firstName?: string; lastName?: string };
  preferenceOrder: number;
  mobilityType: MobilityType;
  academicYear: number;
  /** 1: departure in September, 2: departure in February. */
  term: number;
  programme: { id: number; programmeName?: string };
  country?: { countryCode: string; name?: string } | null;
  partner?: { id: number; fullName?: string } | null;
  submissionDate?: string;
  denialReason?: { id: number; reason?: string } | null;
  cancellationReason?: string | null;
}

/** Body of `POST /api/1.0/mobilityChoice`: the references are sent by id (or country code). */
export interface MobilityChoiceRequest {
  user: { id: number };
  preferenceOrder: number;
  mobilityType: MobilityType;
  academicYear: number;
  term: number;
  programme: { id: number };
  country?: { countryCode: string };
  partner?: { id: number };
}

/** States of a mobility (`MobilityDto.STATE_*`), as the API spells them. */
export const MOBILITY_STATES = {
  created: 'Créée',
  inPreparation: 'En préparation',
  toBePaid: 'A payer',
  inProgress: 'En cours',
  balanceToBePaid: 'Solde à payer',
  closed: 'Terminée',
  cancelled: 'Annulée',
} as const;

/** A document of a mobility (`DocumentDto`): `D` departure document, `R` return document. */
export interface MobilityDocument {
  id: number;
  name: string;
  category: 'D' | 'R';
  filledIn: boolean;
}

/**
 * A mobility (`MobilityDto`): a confirmed mobility choice, with the same id. The list of
 * `GET /api/1.0/mobilities` leaves out the documents and most of the student's data, which the
 * detail (`GET /mobilities/{id}`) carries. Dates are `yyyy-MM-ddTHH:mm:ss[.SSSSSS]`, local time of
 * the server.
 */
export interface Mobility {
  id: number;
  version: number;
  mobilityType: MobilityType;
  academicYear: number;
  term: number;
  state: string;
  stateBeforeCancellation?: string | null;
  programme: { id: number; programmeName?: string; externalSoftName?: string };
  country?: { countryCode: string; name?: string } | null;
  partner?: { id: number; fullName?: string } | null;
  nominatedStudent: Partial<NominatedStudent> & { id: number };
  professorInCharge?: { id?: number; firstName?: string; lastName?: string } | null;
  documents?: MobilityDocument[];
  encodedInProEco: boolean;
  encodedInSecondSoftware: boolean;
  firstPaymentRequestDate?: string | null;
  secondPaymentRequestDate?: string | null;
  denialReason?: { id: number; reason?: string } | null;
  cancellationReason?: string | null;
}

/**
 * Filters of the CSV export of a mobility's documents (`MobilityService.DOCUMENT_FILTER_REGEXP`):
 * departure (`D`) or return (`R`) documents, `F` keeping the filled-in ones; none exports them all.
 */
export type MobilityDocumentFilter = 'D' | 'DF' | 'R' | 'RF';
