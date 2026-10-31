package com.dragomitch.ipl.pae.business.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Asserts on the {@link BusinessException} thrown by a use case: its {@link ErrorCode} and the
 * arguments of its detail message (the values the problem detail reports to the client).
 */
public final class BusinessExceptionAssert {

  private final BusinessException exception;

  private BusinessExceptionAssert(BusinessException exception) {
    this.exception = exception;
  }

  /**
   * Runs a call expected to fail with a business exception.
   *
   * @param call the call
   * @return the assertions on the exception thrown
   */
  public static BusinessExceptionAssert assertThatBusinessException(ThrowingCallable call) {
    BusinessException exception = catchThrowableOfType(call, BusinessException.class);
    assertThat(exception).as("BusinessException thrown").isNotNull();
    return new BusinessExceptionAssert(exception);
  }

  /** The error code of the exception is {@code expected}. */
  public BusinessExceptionAssert hasErrorCode(ErrorCode expected) {
    assertThat(exception.getErrorCode()).isEqualTo(expected);
    return this;
  }

  /** The detail message arguments of the exception are exactly {@code expected}. */
  public BusinessExceptionAssert hasArguments(Object... expected) {
    assertThat(exception.getDetailMessageArguments()).containsExactly(expected);
    return this;
  }
}
