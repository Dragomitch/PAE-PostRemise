package com.dragomitch.ipl.pae.business.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.regex.Pattern;

/** Checks the format and the MOD 97-10 check digits of an {@link Iban}. */
public class IbanValidator implements ConstraintValidator<Iban, CharSequence> {

  /** Longest IBAN of the ISO 13616 registry (Saint Lucia). */
  static final int MAX_LENGTH = 34;

  private static final Pattern FORMAT = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null || value.length() == 0) {
      // reported by @NotBlank
      return true;
    }
    String iban = value.toString().toUpperCase(Locale.ROOT);
    return iban.length() <= MAX_LENGTH && FORMAT.matcher(iban).matches()
        && hasValidChecksum(iban);
  }

  /**
   * ISO 7064 MOD 97-10: the country code and check digits moved to the end, every letter
   * replaced by two digits (A = 10 ... Z = 35), the number modulo 97 must be 1.
   */
  static boolean hasValidChecksum(String iban) {
    String rearranged = iban.substring(4) + iban.substring(0, 4);
    int remainder = 0;
    for (int i = 0; i < rearranged.length(); i++) {
      int digit = Character.digit(rearranged.charAt(i), 36);
      remainder = (digit > 9 ? remainder * 100 : remainder * 10) + digit;
      remainder %= 97;
    }
    return remainder == 1;
  }
}
