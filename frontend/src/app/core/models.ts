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
