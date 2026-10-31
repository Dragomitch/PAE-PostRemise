package com.dragomitch.ipl.pae.business.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The value is an IBAN (ISO 13616): country code, two check digits and the account number,
 * without spaces, whose check digits are right (ISO 7064 MOD 97-10). {@code null} and the
 * empty string are valid (combine with {@code @NotBlank}).
 */
@Documented
@Constraint(validatedBy = IbanValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Iban {

  String message() default "{pae.validation.Iban.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
