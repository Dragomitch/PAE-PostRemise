package com.dragomitch.ipl.pae.business.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The value is a phone number: an optional leading {@code +} followed by digits, possibly
 * grouped with spaces, dots, slashes, dashes or parentheses (e.g. {@code +32 2 123 45 67},
 * {@code 0496/43.33.33}). {@code null} and the empty string are valid (combine with
 * {@code @NotBlank}).
 */
@Documented
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Pattern(regexp = "(\\+?[0-9(][0-9 ./()-]*[0-9])?")
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface PhoneNumber {

  String message() default "{pae.validation.PhoneNumber.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
