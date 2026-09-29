package com.dragomitch.ipl.pae.business.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** The catalogue of error codes and the business exceptions built from it. */
class ErrorCodeTest {

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void everyCodeHasAnUrnTypeAndMessageCodes(ErrorCode code) {
    assertThat(code.type().toString()).matches("urn:pae:problem:[a-z]+(-[a-z]+)*");
    assertThat(code.titleMessageCode()).isEqualTo("problem." + code.name() + ".title");
    assertThat(code.detailMessageCode()).isEqualTo("problem." + code.name() + ".detail");
    assertThat(code.status().isError()).isTrue();
  }

  @Test
  void theTypesAreUnique() {
    List<URI> types = Arrays.stream(ErrorCode.values()).map(ErrorCode::type).toList();
    assertThat(types).doesNotHaveDuplicates();
  }

  @Test
  void theCodesKnownByTheFrontendsExist() {
    assertThat(Arrays.stream(ErrorCode.values()).map(Enum::name)).contains("INVALID_CREDENTIALS",
        "USERNAME_TAKEN", "EMAIL_TAKEN", "VALIDATION_FAILED", "ACCESS_DENIED", "UNAUTHENTICATED",
        "RESOURCE_NOT_FOUND", "CONCURRENT_MODIFICATION", "DATA_CONFLICT", "INTERNAL_ERROR",
        "MALFORMED_REQUEST");
  }

  @ParameterizedTest
  @CsvSource({"USERNAME_TAKEN, 409", "MOBILITY_CHOICE_ALREADY_CONFIRMED, 409",
      "UNKNOWN_OPTION, 422", "PARTNER_OPTION_REQUIRED, 422", "RESOURCE_NOT_FOUND, 404",
      "VALIDATION_FAILED, 400", "INVALID_CREDENTIALS, 401", "ACCESS_DENIED, 403",
      "CONCURRENT_MODIFICATION, 409", "INTERNAL_ERROR, 500"})
  void statusPolicy(ErrorCode code, int status) {
    assertThat(code.status().value()).isEqualTo(status);
  }

  @ParameterizedTest
  @CsvSource({"400, MALFORMED_REQUEST", "401, UNAUTHENTICATED", "403, ACCESS_DENIED",
      "404, RESOURCE_NOT_FOUND", "405, METHOD_NOT_ALLOWED", "406, NOT_ACCEPTABLE",
      "409, DATA_CONFLICT", "413, PAYLOAD_TOO_LARGE", "415, UNSUPPORTED_MEDIA_TYPE",
      "418, MALFORMED_REQUEST", "500, INTERNAL_ERROR", "502, INTERNAL_ERROR",
      "503, SERVICE_UNAVAILABLE"})
  void theCodeOfAnInfrastructureErrorComesFromItsStatus(int status, ErrorCode code) {
    assertThat(ErrorCode.forStatus(status)).isEqualTo(code);
  }

  @Test
  void aBusinessExceptionIsAnErrorResponseOfItsCode() {
    BusinessException ex = new BusinessException(ErrorCode.USERNAME_TAKEN, "jdoe");

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USERNAME_TAKEN);
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    ProblemDetail body = ex.getBody();
    assertThat(body.getType()).isEqualTo(URI.create("urn:pae:problem:username-taken"));
    assertThat(body.getProperties()).containsEntry("code", "USERNAME_TAKEN");
    assertThat(ex.getTitleMessageCode()).isEqualTo("problem.USERNAME_TAKEN.title");
    assertThat(ex.getDetailMessageCode()).isEqualTo("problem.USERNAME_TAKEN.detail");
    assertThat(ex.getDetailMessageArguments()).containsExactly("jdoe");
    assertThat(ex.getMessage()).isEqualTo("USERNAME_TAKEN [jdoe]");
  }

  @Test
  void theSpecialisedExceptionsHaveTheirCode() {
    assertThat(new ResourceNotFoundException().getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    assertThat(new InsufficientPermissionException().getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(new InvalidCredentialsException().getErrorCode())
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    assertThat(new ResourceNotFoundException().getMessage()).isEqualTo("RESOURCE_NOT_FOUND");
  }

  @Test
  void theCauseIsKept() {
    IllegalStateException cause = new IllegalStateException("root");
    assertThat(new BusinessException(ErrorCode.DATA_CONFLICT, cause).getCause()).isSameAs(cause);
  }
}
