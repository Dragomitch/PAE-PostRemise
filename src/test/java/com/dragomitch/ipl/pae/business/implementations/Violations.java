package com.dragomitch.ipl.pae.business.implementations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Reads the violation codes of a {@code checkDataIntegrity()} failure: the business objects
 * report every invalid field at once, as a {@link BusinessException} with the
 * {@link ErrorFormat#INVALID_INPUT_DATA_110} code and one violation code per invalid field.
 */
final class Violations {

  private Violations() {
  }

  /**
   * Runs the check and returns its violation codes, or an empty list when it passes.
   *
   * @param check the integrity check
   * @return the violation codes, in the order they were reported
   */
  @SuppressWarnings("unchecked")
  static List<Integer> of(ThrowingCallable check) {
    BusinessException ex = catchThrowableOfType(check, BusinessException.class);
    if (ex == null) {
      assertThatCode(check).doesNotThrowAnyException();
      return List.of();
    }
    Object errorCode = ReflectionTestUtils.getField(ex, "errorCode");
    if (!Integer.valueOf(ErrorFormat.INVALID_INPUT_DATA_110).equals(errorCode)) {
      throw new AssertionError("Unexpected error code " + errorCode);
    }
    return (List<Integer>) ReflectionTestUtils.getField(ex, "violations");
  }

  /**
   * Returns the error code of a {@link BusinessException} thrown by {@code call}.
   *
   * @param call the call expected to fail
   * @return the error code
   */
  static int errorCodeOf(ThrowingCallable call) {
    BusinessException ex = catchThrowableOfType(call, BusinessException.class);
    if (ex == null) {
      throw new AssertionError("Expected a BusinessException");
    }
    return (Integer) ReflectionTestUtils.getField(ex, "errorCode");
  }
}
