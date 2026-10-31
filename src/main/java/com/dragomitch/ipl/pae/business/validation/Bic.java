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
 * The value is a BIC / SWIFT code (ISO 9362): 4 letters (bank), 2 letters (country), 2 letters
 * or digits (location) and optionally 3 letters or digits (branch), i.e. 8 or 11 characters.
 * {@code null} and the empty string are valid (combine with {@code @NotBlank}).
 */
@Documented
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Pattern(regexp = "([A-Za-z]{6}[A-Za-z0-9]{2}([A-Za-z0-9]{3})?)?")
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Bic {

  String message() default "{pae.validation.Bic.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
