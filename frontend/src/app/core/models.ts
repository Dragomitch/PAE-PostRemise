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
