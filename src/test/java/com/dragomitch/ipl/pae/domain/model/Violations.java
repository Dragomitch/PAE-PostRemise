package com.dragomitch.ipl.pae.domain.model;

import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.dragomitch.ipl.pae.adapter.in.web.mapping.AddressMapper;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.MobilityMapper;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.PartnerMapper;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.ReferenceDataMapper;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.UserMapper;
import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
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
 * Reads the Bean Validation constraints broken by a web DTO (a domain object is validated as its
 * record), as {@code "property.path:Constraint"} strings sorted alphabetically (e.g. {@code
 * "email:Email"}, {@code "option.code:NotBlank"}): the successor of the numeric violation codes the
 * former {@code checkDataIntegrity()} methods reported. Also reads the {@link ErrorCode} of a
 * {@link BusinessException}.
 */
public final class Violations {

  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = FACTORY.getValidator();

  private Violations() {}

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
    return describe(
        VALIDATOR.validate(
            webDtoOf(bean), groups.length == 0 ? new Class<?>[] {Default.class} : groups));
  }

  private static final ReferenceDataMapper REFERENCE_DATA = new ReferenceDataMapper();
  private static final AddressMapper ADDRESSES = new AddressMapper(REFERENCE_DATA);
  private static final UserMapper USERS = new UserMapper(REFERENCE_DATA, ADDRESSES);
  private static final PartnerMapper PARTNERS = new PartnerMapper(REFERENCE_DATA, ADDRESSES);
  private static final MobilityMapper MOBILITIES =
      new MobilityMapper(REFERENCE_DATA, USERS, PARTNERS);

  /**
   * The constraints are those of the web DTOs: a domain object is validated as the record the API
   * reads for it (the domain itself carries no constraint), any other bean as it is.
   */
  private static Object webDtoOf(Object bean) {
    return switch (bean) {
      case NominatedStudent student -> USERS.toDto(student);
      case User user -> USERS.toDto(user);
      case Address address -> ADDRESSES.toDto(address);
      case Mobility mobility -> MOBILITIES.toDto(mobility);
      case MobilityChoice choice -> MOBILITIES.toDto(choice);
      case PartnerOption option -> PARTNERS.toDto(option);
      case Partner partner -> PARTNERS.toDto(partner);
      case Option option -> REFERENCE_DATA.toDto(option);
      case Programme programme -> REFERENCE_DATA.toDto(programme);
      case Country country -> REFERENCE_DATA.toDto(country);
      case DenialReason reason -> REFERENCE_DATA.toDto(reason);
      case Document document -> MOBILITIES.toDto(document);
      case Payment payment -> MOBILITIES.toDto(payment);
      default -> bean;
    };
  }

  /** Validates a bean as a creation: {@link Default} and {@link OnCreate} groups. */
  public static List<String> onCreate(Object bean) {
    return of(bean, Default.class, OnCreate.class);
  }

  /**
   * The broken constraints of a method validation failure, with their full path ({@code
   * "signup.user.email:Email"}).
   *
   * @param call the call expected to fail
   * @return the broken constraints, sorted
   */
  public static List<String> thrownBy(ThrowingCallable call) {
    ConstraintViolationException ex =
        catchThrowableOfType(ConstraintViolationException.class, call);
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
    BusinessException ex = catchThrowableOfType(BusinessException.class, call);
    if (ex == null) {
      throw new AssertionError("Expected a BusinessException");
    }
    return ex.getErrorCode();
  }

  private static List<String> describe(Collection<? extends ConstraintViolation<?>> violations) {
    return violations.stream()
        .map(v ->
            v.getPropertyPath()
                + ":"
                + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
        .sorted()
        .toList();
  }
}
