package com.dragomitch.ipl.pae.business.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.dto.UserDto;

import jakarta.validation.constraints.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The constraints written for the application ({@link Iban}, {@link Bic}, {@link PhoneNumber},
 * {@link FilterValueRequired}) and the email format, formerly the {@code isAValid*} methods of
 * {@code DataValidationUtils}.
 */
class CustomConstraintsTest {

  record IbanHolder(@Iban String value) {
  }

  record BicHolder(@Bic String value) {
  }

  record PhoneHolder(@PhoneNumber String value) {
  }

  record EmailHolder(@Email(regexp = UserDto.EMAIL_REGEXP) String value) {
  }

  // ----- IBAN -----

  @ParameterizedTest
  @ValueSource(strings = {
      "BE68539007547034",                 // 16, Belgium
      "BE71096123456769",                 // 16, Belgium (the fixture IBAN of the tests)
      "NO9386011117947",                  // 15, the shortest IBANs
      "DE89370400440532013000",           // 22, Germany
      "FR1420041010050500013M02606",      // 27, France, letter in the account number
      "MT84MALT011000012345MTLCAST001S",  // 31
      "LC55HEMM000100010012001200023015", // 32, Saint Lucia
      "be68539007547034"})                // lower case is accepted
  void validIbans(String iban) {
    assertThat(Violations.of(new IbanHolder(iban))).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void nullAndEmptyIbansAreLeftToNotBlank(String iban) {
    assertThat(Violations.of(new IbanHolder(iban))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "BE68539007547035",                   // wrong check digits
      "BE92732143130176",                   // wrong check digits
      "BE68 5390 0754 7034",                // spaces
      "1E68539007547034",                   // country code must be letters
      "BEXX539007547034",                   // check digits must be digits
      "BE6853900754",                       // too short
      "NO938601111794",                     // 14 characters: shorter than any IBAN
      "BE86539007547034",                   // wrong check digits (swapped)
      "DE89370400440532013001",             // wrong check digits (account changed)
      "BE96001244289402",                   // the former fixture IBAN, wrong check digits
      "BE68-5390",                          // forbidden character, too short
      "LC55HEMM000100010012001200023015123", // 35 characters: longer than any IBAN
      "BE9?001244289402"})                  // forbidden character
  void invalidIbans(String iban) {
    assertThat(Violations.of(new IbanHolder(iban))).containsExactly("value:Iban");
  }

  @Test
  void theChecksumIsModulo97() {
    assertThat(IbanValidator.hasValidChecksum("GB29NWBK60161331926819")).isTrue();
    assertThat(IbanValidator.hasValidChecksum("GB28NWBK60161331926819")).isFalse();
  }

  // ----- BIC -----

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"GKCCBEBB", "PSSTFRPPXXX", "gkccbebb", "GEBABEBB", "DEUTDEFF500"})
  void validBics(String bic) {
    assertThat(Violations.of(new BicHolder(bic))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"GKCCBEB", "GKCCBEBB1", "PSSTFRPPXXXX", "1KCCBEBB", "GKCC-BEBB",
      "KRIBEBEBB", "KREDBEB", "123hello", "B093"})
  void invalidBics(String bic) {
    assertThat(Violations.of(new BicHolder(bic))).containsExactly("value:Bic");
  }

  // ----- phone numbers -----

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"0472/12.34.56", "+32493184140", "0472 12 34 56", "+32 (0)2 123-45-67",
      "0496/43/33/33"})
  void validPhoneNumbers(String phoneNumber) {
    assertThat(Violations.of(new PhoneHolder(phoneNumber))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello", "+", "0472-", "+32 abc", "++32472", "0472#123"})
  void invalidPhoneNumbers(String phoneNumber) {
    assertThat(Violations.of(new PhoneHolder(phoneNumber))).containsExactly("value:PhoneNumber");
  }

  // ----- email addresses -----

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"hello_21@gmail.com", "a.b-c@mail.example.org", "a+tag@example.org", "x@d-1.be"})
  void validEmails(String email) {
    assertThat(Violations.of(new EmailHolder(email))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello", "hello@", "@gmail.com", "hello@gmail", "hello@gmail.c",
      "hel lo@gmail.com", "hello@@gmail.com"})
  void invalidEmails(String email) {
    assertThat(Violations.of(new EmailHolder(email))).containsExactly("value:Email");
  }

  // ----- partner search (class-level constraint) -----

  @ParameterizedTest
  @CsvSource(value = {"NULL, NULL", "NULL, anything", "archived, ACME", "country, BE"},
      nullValues = "NULL")
  void validPartnerSearches(String filter, String value) {
    assertThat(Violations.of(new PartnerSearch(filter, value))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource(value = {"archived, NULL", "country, ''", "country, '  '"}, nullValues = "NULL")
  void aFilterNeedsAValue(String filter, String value) {
    assertThat(Violations.of(new PartnerSearch(filter, value)))
        .containsExactly("value:FilterValueRequired");
  }

  @Test
  void anUnknownFilterIsRejected() {
    assertThat(Violations.of(new PartnerSearch("all", "x"))).containsExactly("filter:Pattern");
  }

  @Test
  void theDefaultSearchHasNoFilter() {
    assertThat(PartnerSearch.all()).isEqualTo(new PartnerSearch(null, null));
  }
}
