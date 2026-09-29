package com.dragomitch.ipl.pae.business;

import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import java.util.Collection;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Reads the Bean Validation constraints broken by a DTO, as {@code "property.path:Constraint"}
 * strings sorted alphabetically (e.g. {@code "email:Email"}, {@code "option.code:NotBlank"}): the
 * successor of the numeric violation codes the former {@code checkDataIntegrity()} methods
 * reported. Also reads the {@link ErrorCode} of a {@link BusinessException}.
 */
public final class Violations {

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = FACTORY.getValidator();

  private Violations() {
  }

  /** A standalone validator (the Spring-configured one is tested separately). */
  public static Validator validator() {
    return VALIDATOR;
  }

  /**
   * Validates a bean.
   *
   * @param bean the bean
   * @param groups the groups, {@link Default} when none
   * @return the broken constraints, sorted; empty when the bean is valid
   */
  public static List<String> of(Object bean, Class<?>... groups) {
    return describe(VALIDATOR.validate(bean, groups.length == 0 ? new Class<?>[] {Default.class}
        : groups));
  }

  /** Validates a bean as a creation: {@link Default} and {@link OnCreate} groups. */
  public static List<String> onCreate(Object bean) {
    return of(bean, Default.class, OnCreate.class);
  }

  /**
   * The broken constraints of a method validation failure, with their full path
   * ({@code "signup.user.email:Email"}).
   *
   * @param call the call expected to fail
   * @return the broken constraints, sorted
   */
  public static List<String> thrownBy(ThrowingCallable call) {
    ConstraintViolationException ex =
        catchThrowableOfType(call, ConstraintViolationException.class);
    if (ex == null) {
      throw new AssertionError("Expected a ConstraintViolationException");
    }
    return describe(ex.getConstraintViolations());
  }

  /**
   * Returns the error code of a {@link BusinessException} thrown by {@code call}.
   *
   * @param call the call expected to fail
   * @return the error code
   */
  public static ErrorCode errorCodeOf(ThrowingCallable call) {
    BusinessException ex = catchThrowableOfType(call, BusinessException.class);
    if (ex == null) {
      throw new AssertionError("Expected a BusinessException");
    }
    return ex.getErrorCode();
  }

  private static List<String> describe(Collection<? extends ConstraintViolation<?>> violations) {
    return violations.stream()
        .map(v -> v.getPropertyPath() + ":"
            + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
        .sorted()
        .toList();
  }
}
