export interface Credentials {
  username: string;
  password: string;
}

export interface OptionRef {
  code: string;
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
