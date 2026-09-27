package com.dragomitch.ipl.pae.business.exceptions;

import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;

/**
 * Catalogue of the errors of the API. Every error response is an RFC 9457 problem
 * ({@code application/problem+json}) whose {@code code} property is the name of one of these
 * constants: it is the stable, machine-readable key clients branch on (never the message text).
 *
 * <p>Each code carries:
 * <ul>
 * <li>its HTTP status, following one policy:
 *   <ul>
 *   <li>400: the request is malformed or breaks a validation constraint
 *   ({@link #VALIDATION_FAILED}, with the offending fields in {@code errors});</li>
 *   <li>401 / 403: authentication and authorization;</li>
 *   <li>404: the resource identified by the URL does not exist;</li>
 *   <li>409: the request conflicts with the current state of the data (uniqueness, state of a
 *   mobility or of a mobility choice, stale version);</li>
 *   <li>422: the request is well-formed but breaks a business rule, e.g. it refers to an entity
 *   that does not exist (unknown option, country, user...) or that is not allowed for the
 *   requester;</li>
 *   <li>5xx: server-side failures, never detailed to the client.</li>
 *   </ul></li>
 * <li>its problem {@code type}, {@code urn:pae:problem:<kebab-case-code>};</li>
 * <li>its localized title and detail, read from the {@code MessageSource}
 * ({@code i18n/messages*.properties}) under {@code problem.<CODE>.title} and
 * {@code problem.<CODE>.detail}. A test checks that every code is translated in every
 * supported language.</li>
 * </ul>
 *
 * <p>The field-level checks of the former numeric catalogue ({@code errors.json}: invalid first
 * name, too long username, malformed IBAN...) are now Bean Validation constraints reported
 * together under {@link #VALIDATION_FAILED}.
 */
public enum ErrorCode {

  // ----- generic errors -----
  /** Unexpected server-side failure (formerly 100). The problem carries an {@code errorId}. */
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
  /** A dependency of the request timed out or is unavailable. */
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
  /** Missing, invalid or expired session (formerly 101). */
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
  /** Sign-in with an unknown username or a wrong password (formerly 101 as well). */
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
  /** Authenticated but not allowed, or CSRF token missing or invalid (formerly 103). */
  ACCESS_DENIED(HttpStatus.FORBIDDEN),
  /** The resource of the URL does not exist, or the URL matches no endpoint (formerly 104). */
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
  /** One or more validation constraints failed, listed in {@code errors} (formerly 110, 130). */
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
  /** Unreadable body (malformed JSON), missing or wrongly typed parameter. */
  MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
  /** HTTP method not supported by the endpoint. */
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
  /** None of the media types of the {@code Accept} header can be produced. */
  NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE),
  /** The {@code Content-Type} of the body is not supported. */
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  /** The request body is too large. */
  PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
  /** The entity was modified by someone else since it was read (formerly 120, then 400). */
  CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
  /** The database refused the change (integrity constraint). */
  DATA_CONFLICT(HttpStatus.CONFLICT),

  // ----- users -----
  /** Username already used by another account (formerly 204). */
  USERNAME_TAKEN(HttpStatus.CONFLICT),
  /** Email address already used by another account (formerly 207). */
  EMAIL_TAKEN(HttpStatus.CONFLICT),
  /** The option of the user or of the partner does not exist (formerly 210). */
  UNKNOWN_OPTION(HttpStatus.UNPROCESSABLE_ENTITY),
  /** The user referenced by the request does not exist (formerly 200). */
  UNKNOWN_USER(HttpStatus.UNPROCESSABLE_ENTITY),

  // ----- mobility choices -----
  /** The mobility choice already became a mobility (formerly 301 and 321). */
  MOBILITY_CHOICE_ALREADY_CONFIRMED(HttpStatus.CONFLICT),
  /** The mobility choice was already cancelled or rejected (formerly 317). */
  MOBILITY_CHOICE_CLOSED(HttpStatus.CONFLICT),
  /** A mobility choice cannot be confirmed before a partner is chosen (formerly 324). */
  PARTNER_REQUIRED_TO_CONFIRM(HttpStatus.CONFLICT),
  /** A professor cannot apply for a mobility himself (formerly 322). */
  PROFESSOR_CANNOT_APPLY(HttpStatus.UNPROCESSABLE_ENTITY),
  /** The partner is not in the country of the mobility choice (formerly 320). */
  COUNTRY_CHANGE_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY),
  /** The country referenced by the request does not exist (formerly 900). */
  UNKNOWN_COUNTRY(HttpStatus.UNPROCESSABLE_ENTITY),
  /** The programme referenced by the request does not exist (formerly 1000). */
  UNKNOWN_PROGRAMME(HttpStatus.UNPROCESSABLE_ENTITY),
  /** The denial reason referenced by the request does not exist (formerly 134 and 400). */
  UNKNOWN_DENIAL_REASON(HttpStatus.UNPROCESSABLE_ENTITY),

  // ----- mobilities -----
  /** The mobility was cancelled (formerly 501). */
  MOBILITY_CANCELLED(HttpStatus.CONFLICT),
  /** The mobility is closed (formerly 502). */
  MOBILITY_CLOSED(HttpStatus.CONFLICT),
  /** Some departure documents are not filled in (formerly 503). */
  DEPARTURE_DOCUMENTS_INCOMPLETE(HttpStatus.CONFLICT),
  /** Some return documents are not filled in (formerly 504). */
  RETURN_DOCUMENTS_INCOMPLETE(HttpStatus.CONFLICT),
  /** Some documents are not filled in (formerly 507). */
  DOCUMENTS_INCOMPLETE(HttpStatus.CONFLICT),
  /** The document is not one of the documents of the mobility (formerly 505). */
  UNKNOWN_DOCUMENT(HttpStatus.UNPROCESSABLE_ENTITY),
  /** The bank details of the nominated student are missing (formerly 506). */
  INCOMPLETE_BANK_DETAILS(HttpStatus.CONFLICT),
  /** No payment can be requested in the current state of the mobility (formerly 110). */
  PAYMENT_NOT_EXPECTED(HttpStatus.CONFLICT),
  /** A professor must give a denial reason to cancel a mobility (formerly 130). */
  DENIAL_REASON_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY),
  /** A student must explain why he cancels a mobility (formerly 130). */
  CANCELLATION_REASON_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY),

  // ----- nominated students -----
  /** The personal data of the student were already recorded (formerly 618). */
  ALREADY_NOMINATED(HttpStatus.CONFLICT),

  // ----- partners -----
  /** A partner with mobility choices cannot be archived (formerly 710). */
  PARTNER_HAS_MOBILITY_CHOICES(HttpStatus.CONFLICT),
  /** Only an archived partner can be restored (formerly 711). */
  PARTNER_NOT_ARCHIVED(HttpStatus.CONFLICT),
  /** A partner must always offer at least one option (formerly 712). */
  PARTNER_OPTION_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY);

  /** Prefix of the problem {@code type} URIs. */
  public static final String TYPE_PREFIX = "urn:pae:problem:";

  private static final String MESSAGE_PREFIX = "problem.";

  private final HttpStatus status;

  ErrorCode(HttpStatus status) {
    this.status = status;
  }

  /** The HTTP status of the responses carrying this error. */
  public HttpStatus status() {
    return status;
  }

  /** The problem {@code type}: {@code urn:pae:problem:<kebab-case-code>}. */
  public URI type() {
    return URI.create(TYPE_PREFIX + name().toLowerCase(Locale.ROOT).replace('_', '-'));
  }

  /** Message code of the localized problem title: {@code problem.<CODE>.title}. */
  public String titleMessageCode() {
    return MESSAGE_PREFIX + name() + ".title";
  }

  /** Message code of the localized problem detail: {@code problem.<CODE>.detail}. */
  public String detailMessageCode() {
    return MESSAGE_PREFIX + name() + ".detail";
  }

  /**
   * The code whose status is {@code status}, for errors raised by the infrastructure (Spring
   * MVC, servlet container) that carry no code of their own.
   *
   * @param status an HTTP error status
   * @return the matching generic code ({@link #MALFORMED_REQUEST} for any other 4xx,
   *     {@link #INTERNAL_ERROR} for any other status)
   */
  public static ErrorCode forStatus(int status) {
    return switch (status) {
      case 401 -> UNAUTHENTICATED;
      case 403 -> ACCESS_DENIED;
      case 404 -> RESOURCE_NOT_FOUND;
      case 405 -> METHOD_NOT_ALLOWED;
      case 406 -> NOT_ACCEPTABLE;
      case 409 -> DATA_CONFLICT;
      case 413 -> PAYLOAD_TOO_LARGE;
      case 415 -> UNSUPPORTED_MEDIA_TYPE;
      case 503 -> SERVICE_UNAVAILABLE;
      default -> status >= 400 && status < 500 ? MALFORMED_REQUEST : INTERNAL_ERROR;
    };
  }
}
