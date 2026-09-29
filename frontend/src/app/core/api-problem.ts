import { HttpErrorResponse } from '@angular/common/http';
import { MonoTypeOperatorFunction, TimeoutError, catchError, throwError } from 'rxjs';

import { ProblemDetail, ProblemFieldError, isProblemDetail, isProblemFieldError } from './problem-detail';

/**
 * Machine codes produced by the frontend itself when the response is not a usable Problem
 * Details body (network failure, proxy error page, non-JSON body, timeout...).
 * Backend codes (USERNAME_TAKEN, ...) are passed through unchanged.
 */
export const ClientProblemCode = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  SERVER_ERROR: 'SERVER_ERROR',
  UNAUTHENTICATED: 'UNAUTHENTICATED',
  ACCESS_DENIED: 'ACCESS_DENIED',
  NOT_FOUND: 'NOT_FOUND',
  UNKNOWN: 'UNKNOWN',
} as const;

export interface ApiProblemInit {
  status: number;
  code: string;
  type?: string;
  title: string;
  detail: string;
  instance?: string;
  errors?: readonly ProblemFieldError[];
  fromServer: boolean;
}

/**
 * Normalised, always user-displayable error for any failed API call. Services emit it (through
 * `mapApiProblem()`) instead of `HttpErrorResponse`, so components never deal with raw bodies.
 */
export class ApiProblem {
  /** HTTP status, `0` when no response was received. */
  readonly status: number;
  /** Stable UPPER_SNAKE key: the backend `code`, or one of `ClientProblemCode`. */
  readonly code: string;
  readonly type: string;
  /** Localized short summary. */
  readonly title: string;
  /** Localized message to show to the user (backend `detail` when available). */
  readonly detail: string;
  readonly instance?: string;
  /** Field-level validation errors (empty unless the backend reported some). */
  readonly errors: readonly ProblemFieldError[];
  /** `errors` grouped by field name, for display next to form inputs. */
  readonly fieldErrors: Readonly<Record<string, readonly string[]>>;
  /** `true` when the content comes from a valid backend problem+json body. */
  readonly fromServer: boolean;

  constructor(init: ApiProblemInit) {
    this.status = init.status;
    this.code = init.code;
    this.type = init.type ?? 'about:blank';
    this.title = init.title;
    this.detail = init.detail;
    this.instance = init.instance;
    this.errors = init.errors ?? [];
    this.fromServer = init.fromServer;
    const grouped: Record<string, string[]> = {};
    for (const error of this.errors) {
      (grouped[error.field] ??= []).push(error.message);
    }
    this.fieldErrors = grouped;
  }

  /** Messages for one field joined into a single displayable string, or `null`. */
  fieldError(field: string): string | null {
    const messages = this.fieldErrors[field];
    return messages ? messages.join(' ') : null;
  }
}

/**
 * Localized messages for well-known backend codes. Used ONLY when the backend problem has no
 * usable `detail`: the backend's localized detail always wins.
 */
export function knownCodeMessage(code: string): string | undefined {
  switch (code) {
    case 'INVALID_CREDENTIALS':
      return $localize`:@@problem.code.invalidCredentials:Nom d'utilisateur et/ou mot de passe incorrect.`;
    case 'USERNAME_TAKEN':
      return $localize`:@@problem.code.usernameTaken:Ce nom d'utilisateur est déjà utilisé.`;
    case 'EMAIL_TAKEN':
      return $localize`:@@problem.code.emailTaken:Cette adresse e-mail est déjà utilisée.`;
    case 'VALIDATION_FAILED':
      return $localize`:@@problem.code.validationFailed:Certaines informations sont invalides. Corrigez les champs indiqués.`;
    case 'ACCESS_DENIED':
      return $localize`:@@problem.code.accessDenied:Vous n'avez pas les droits nécessaires pour effectuer cette action.`;
    case 'UNAUTHENTICATED':
      return $localize`:@@problem.code.unauthenticated:Votre session a expiré. Veuillez vous reconnecter.`;
    default:
      return undefined;
  }
}

/** Frontend fallback (code + localized message) chosen from the HTTP status alone. */
export function statusFallback(status: number): { code: string; message: string } {
  if (status === 0) {
    return {
      code: ClientProblemCode.NETWORK_ERROR,
      message: $localize`:@@problem.fallback.network:Impossible de joindre le serveur. Vérifiez votre connexion puis réessayez.`,
    };
  }
  if (status === 401) {
    return { code: ClientProblemCode.UNAUTHENTICATED, message: knownCodeMessage('UNAUTHENTICATED')! };
  }
  if (status === 403) {
    return { code: ClientProblemCode.ACCESS_DENIED, message: knownCodeMessage('ACCESS_DENIED')! };
  }
  if (status === 404) {
    return {
      code: ClientProblemCode.NOT_FOUND,
      message: $localize`:@@problem.fallback.notFound:La ressource demandée est introuvable.`,
    };
  }
  if (status === 408 || status === 504) {
    return timeoutFallback();
  }
  if (status === 502 || status === 503) {
    return {
      code: ClientProblemCode.SERVICE_UNAVAILABLE,
      message: $localize`:@@problem.fallback.unavailable:Le service est momentanément indisponible. Réessayez dans quelques instants.`,
    };
  }
  if (status >= 500) {
    return {
      code: ClientProblemCode.SERVER_ERROR,
      message: $localize`:@@problem.fallback.server:Une erreur est survenue sur le serveur. Réessayez plus tard.`,
    };
  }
  return unknownFallback();
}

function timeoutFallback(): { code: string; message: string } {
  return {
    code: ClientProblemCode.TIMEOUT,
    message: $localize`:@@problem.fallback.timeout:Le serveur a mis trop de temps à répondre. Réessayez.`,
  };
}

function unknownFallback(): { code: string; message: string } {
  return {
    code: ClientProblemCode.UNKNOWN,
    message: $localize`:@@problem.fallback.unknown:Une erreur inattendue est survenue. Réessayez.`,
  };
}

const HTML_PATTERN = /<\/?[a-z][^>]*>/i;
const STACK_FRAME_PATTERN = /\n\s+at\s|Exception:/;

/** Rejects empty strings and anything that looks like markup or a stack trace. */
function displayable(text: string | null | undefined): string | undefined {
  if (typeof text !== 'string') {
    return undefined;
  }
  const trimmed = text.trim();
  if (!trimmed || HTML_PATTERN.test(trimmed) || STACK_FRAME_PATTERN.test(trimmed)) {
    return undefined;
  }
  return trimmed;
}

/** Returns the parsed error body, parsing JSON text when the response declared a JSON type. */
function readBody(error: HttpErrorResponse): unknown {
  const body: unknown = error.error;
  if (typeof body !== 'string') {
    return body;
  }
  const contentType = error.headers?.get('Content-Type') ?? '';
  if (!/json/i.test(contentType)) {
    return undefined;
  }
  try {
    return JSON.parse(body);
  } catch {
    return undefined;
  }
}

function fromProblem(status: number, problem: ProblemDetail): ApiProblem {
  const effectiveStatus = status || problem.status || 0;
  const fallback = statusFallback(effectiveStatus);
  const code = displayable(problem.code) ?? fallback.code;
  const errors = (problem.errors ?? []).filter(isProblemFieldError);
  const detail =
    displayable(problem.detail) ?? knownCodeMessage(code) ?? displayable(problem.title) ?? fallback.message;
  return new ApiProblem({
    status: effectiveStatus,
    code,
    type: displayable(problem.type),
    title: displayable(problem.title) ?? detail,
    detail,
    instance: displayable(problem.instance),
    errors,
    fromServer: true,
  });
}

function fromFallback(status: number, fallback: { code: string; message: string }): ApiProblem {
  return new ApiProblem({
    status,
    code: fallback.code,
    title: fallback.message,
    detail: fallback.message,
    fromServer: false,
  });
}

function isTimeout(error: unknown): boolean {
  return (
    error instanceof TimeoutError ||
    (typeof error === 'object' && error !== null && (error as { name?: unknown }).name === 'TimeoutError')
  );
}

/**
 * Turns anything thrown by an API call into an `ApiProblem`:
 * - a valid problem+json body is used as is (localized by the backend);
 * - anything else (status 0, proxy HTML page, non-JSON body, timeout, unexpected exception)
 *   gets a localized frontend fallback keyed by status/category. Raw bodies are never exposed.
 */
export function toApiProblem(error: unknown): ApiProblem {
  if (error instanceof ApiProblem) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 && isTimeout(error.error)) {
      return fromFallback(0, timeoutFallback());
    }
    const body = readBody(error);
    if (isProblemDetail(body)) {
      return fromProblem(error.status, body);
    }
    return fromFallback(error.status, statusFallback(error.status));
  }
  if (isTimeout(error)) {
    return fromFallback(0, timeoutFallback());
  }
  return fromFallback(0, unknownFallback());
}

/** RxJS operator used by services: re-throws every error as an `ApiProblem`. */
export function mapApiProblem<T>(): MonoTypeOperatorFunction<T> {
  return catchError((error: unknown) => throwError(() => toApiProblem(error)));
}
