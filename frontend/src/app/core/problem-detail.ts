/**
 * RFC 9457 "Problem Details for HTTP APIs" as returned by the EMA backend
 * (`Content-Type: application/problem+json`) for every error response.
 *
 * `title`, `detail` and `errors[].message` are already localized by the backend according to
 * the `Accept-Language` request header (see `acceptLanguageInterceptor`).
 */
export interface ProblemDetail {
  /** `urn:pae:problem:<kebab-code>` or `about:blank`. */
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Stable UPPER_SNAKE machine key (e.g. `USERNAME_TAKEN`); always sent by the EMA backend. */
  code?: string;
  /** Only present for validation errors. */
  errors?: ProblemFieldError[];
}

export interface ProblemFieldError {
  /** Name of the offending property, matching the request body (e.g. `email`, `option.code`). */
  field: string;
  /** Validation constraint name (e.g. `Email`, `NotBlank`). */
  code?: string;
  /** Localized, user-displayable message. */
  message: string;
}

export const PROBLEM_JSON_MEDIA_TYPE = 'application/problem+json';

function isOptionalString(value: unknown): boolean {
  return value === undefined || value === null || typeof value === 'string';
}

/** Plain objects only (as produced by JSON.parse): rejects arrays and DOM objects such as the
 *  `ProgressEvent` HttpClient reports for network errors (which has a string `type`). */
function isRecord(value: unknown): value is Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
}

/** Type guard for a single entry of `ProblemDetail.errors`. */
export function isProblemFieldError(value: unknown): value is ProblemFieldError {
  return (
    isRecord(value) &&
    typeof value['field'] === 'string' &&
    typeof value['message'] === 'string' &&
    isOptionalString(value['code'])
  );
}

/**
 * Type guard for a Problem Details body. RFC 9457 makes every member optional, so the body is
 * accepted when it is a JSON object whose known members have the right types and that carries
 * at least one identifying member (`code`, `title`, `detail` or `type`). Invalid `errors`
 * entries do not reject the problem: they are filtered out by the mapper.
 */
export function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!isRecord(value)) {
    return false;
  }
  const typesOk =
    isOptionalString(value['type']) &&
    isOptionalString(value['title']) &&
    isOptionalString(value['detail']) &&
    isOptionalString(value['instance']) &&
    isOptionalString(value['code']) &&
    (value['status'] === undefined || value['status'] === null || typeof value['status'] === 'number') &&
    (value['errors'] === undefined || value['errors'] === null || Array.isArray(value['errors']));
  if (!typesOk) {
    return false;
  }
  return ['code', 'title', 'detail', 'type'].some(
    (key) => typeof value[key] === 'string' && (value[key] as string).length > 0,
  );
}
