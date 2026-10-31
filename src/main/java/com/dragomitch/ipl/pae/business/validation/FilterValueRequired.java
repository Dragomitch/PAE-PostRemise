package com.dragomitch.ipl.pae.business.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.dragomitch.ipl.pae.business.dto.PartnerSearch;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.util.StringUtils;

/**
 * Class-level (cross-field) constraint of a {@link PartnerSearch}: when a {@code filter} is
 * chosen, its {@code value} is required. The violation is reported on the {@code value}
 * property.
 */
@Documented
@Constraint(validatedBy = FilterValueRequired.Validator.class)
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface FilterValueRequired {

  String message() default "{pae.validation.FilterValueRequired.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** Validator of {@link FilterValueRequired}. */
  class Validator implements ConstraintValidator<FilterValueRequired, PartnerSearch> {

    @Override
    public boolean isValid(PartnerSearch search, ConstraintValidatorContext context) {
      if (search == null || search.filter() == null || StringUtils.hasText(search.value())) {
        return true;
      }
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
          .addPropertyNode("value")
          .addConstraintViolation();
      return false;
    }
  }
}
