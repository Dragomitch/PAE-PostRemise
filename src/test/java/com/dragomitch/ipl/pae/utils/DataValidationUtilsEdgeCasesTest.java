package com.dragomitch.ipl.pae.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Boundary cases of {@link DataValidationUtils} not covered by TestDataValidationUtils: length
 * limits, negative limits, the IBAN length and check digits and the BIC length rules on top of
 * their regular expressions.
 */
class DataValidationUtilsEdgeCasesTest {

  @ParameterizedTest(name = "\"{0}\" <= {1}: {2}")
  @CsvSource(value = {
      "abc, 3, true",
      "abc, 4, true",
      "abc, 2, false",
      "'', 0, true",
      "abc, -1, false",
      "'', -1, false",
      "NULL, 10, false"}, nullValues = "NULL")
  void isShortherOrEqualsThan(String value, int max, boolean expected) {
    assertThat(DataValidationUtils.isShortherOrEqualsThan(value, max)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "BE68539007547034",                 // 16, Belgium
      "NO9386011117947",                  // 15, the shortest IBANs
      "DE89370400440532013000",           // 22, Germany
      "FR1420041010050500013M02606",      // 27, France, letter in the account number
      "MT84MALT011000012345MTLCAST001S",  // 31, the column maximum
      "be68539007547034"})                // lower case is accepted
  void validIbans(String iban) {
    assertThat(DataValidationUtils.isAValidIban(iban)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
      "MT84MALT011000012345MTLCAST001S1", // 32 characters: longer than the column
      "BE68 5390 0754 7034",              // spaces
      "1E68539007547034",                 // country code must be letters
      "BEXX539007547034",                 // check digits must be digits
      "BE6853900754",                     // too short
      "NO938601111794",                   // 14 characters: too short
      "BE68539007547035",                 // wrong check digits (last digit changed)
      "BE86539007547034",                 // wrong check digits (swapped)
      "DE89370400440532013001",           // wrong check digits
      "BE68-5390"})
  void invalidIbans(String iban) {
    assertThat(DataValidationUtils.isAValidIban(iban)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"GKCCBEBB", "PSSTFRPPXXX", "gkccbebb"})
  void validBics(String bic) {
    assertThat(DataValidationUtils.isAValidBic(bic)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"GKCCBEB", "GKCCBEBB1", "PSSTFRPPXXXX", "1KCCBEBB", "GKCC-BEBB"})
  void invalidBics(String bic) {
    assertThat(DataValidationUtils.isAValidBic(bic)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({"0, false, true", "1, true, true", "-1, false, false",
      "2147483647, true, true", "-2147483648, false, false"})
  void positiveChecks(int number, boolean positive, boolean positiveOrZero) {
    assertThat(DataValidationUtils.isPositive(number)).isEqualTo(positive);
    assertThat(DataValidationUtils.isPositiveOrZero(number)).isEqualTo(positiveOrZero);
  }

  @Test
  void theUtilityClassCannotBeInstantiated() throws Exception {
    Constructor<DataValidationUtils> constructor =
        DataValidationUtils.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance).isInstanceOf(InvocationTargetException.class)
        .cause().isInstanceOf(UnsupportedOperationException.class);
  }
}
