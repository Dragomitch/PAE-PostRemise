package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.BANK_NAME_MAX_LENGTH_OVERFLOW_616;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.CARD_HOLDER_MAX_LENGTH_OVERFLOW_614;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.EXISTENCE_VIOLATION_COUNTRY_CODE_900;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INCOMPLETE_BANK_DETAILS_506;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_ADDRESS_NULL_608;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_BANK_NAME_615;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_BIC_617;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_BIRTHDATE_605;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_BIRTHDATE_NULL_604;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_GENDER_610;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_GENDER_VALUE_611;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_IBAN_613;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_NATIONALITY_NULL_606;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_NUMBER_OF_PASSED_YEARS_612;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_PHONE_NUMBER_619;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_TITLE_601;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_TITLE_VALUE_602;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.MAX_LENGTH_COUNTRY_CODE_OVERFLOW_314;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.PHONE_NUMBER_MAX_LENGTH_OVERFLOW_609;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.TITLE_MAX_LENGTH_OVERFLOW_603;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Validation rules of a nominated student, one field at a time. */
class NominatedStudentImplTest {

  private final EntityFactoryImpl factory = new EntityFactoryImpl();
  private NominatedStudent student;

  @BeforeEach
  void validStudent() {
    student = (NominatedStudent) factory.build(NominatedStudent.class);
    student.setFirstName("Alice");
    student.setLastName("Martin");
    student.setUsername("alice");
    student.setPassword("secret");
    student.setEmail("alice@student.test");
    OptionDto option = (OptionDto) factory.build(OptionDto.class);
    option.setCode("BIN");
    student.setOption(option);
    student.setTitle("Ms");
    student.setBirthdate(LocalDate.of(2001, 4, 12));
    CountryDto nationality = (CountryDto) factory.build(CountryDto.class);
    nationality.setCountryCode("BE");
    student.setNationality(nationality);
    student.setAddress((AddressDto) factory.build(AddressDto.class));
    student.setPhoneNumber("+32470000001");
    student.setGender("F");
    student.setNbrPassedYears(2);
    student.setIban("BE68539007547034");
    student.setCardHolder("Alice Martin");
    student.setBankName("Belfius");
    student.setBic("GKCCBEBB");
  }

  @Test
  void aCompleteStudentIsValid() {
    assertThat(Violations.of(student::checkDataIntegrity)).isEmpty();
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        invalid("no title", s -> s.setTitle(null), INVALID_TITLE_601),
        invalid("empty title", s -> s.setTitle(""), INVALID_TITLE_601),
        invalid("title too long", s -> s.setTitle("Miss"), TITLE_MAX_LENGTH_OVERFLOW_603),
        invalid("unknown title", s -> s.setTitle("Dr"), INVALID_TITLE_VALUE_602),
        invalid("no birthdate", s -> s.setBirthdate(null), INVALID_BIRTHDATE_NULL_604),
        invalid("born today", s -> s.setBirthdate(LocalDate.now()), INVALID_BIRTHDATE_605),
        invalid("born tomorrow", s -> s.setBirthdate(LocalDate.now().plusDays(1)),
            INVALID_BIRTHDATE_605),
        invalid("no nationality", s -> s.setNationality(null), INVALID_NATIONALITY_NULL_606),
        invalid("nationality without code", s -> s.getNationality().setCountryCode(null),
            EXISTENCE_VIOLATION_COUNTRY_CODE_900),
        invalid("nationality code of 3 letters", s -> s.getNationality().setCountryCode("BEL"),
            MAX_LENGTH_COUNTRY_CODE_OVERFLOW_314),
        invalid("no address", s -> s.setAddress(null), INVALID_ADDRESS_NULL_608),
        invalid("no phone number", s -> s.setPhoneNumber(""), INVALID_PHONE_NUMBER_619),
        invalid("phone number too long", s -> s.setPhoneNumber("+3247000000000001"),
            PHONE_NUMBER_MAX_LENGTH_OVERFLOW_609),
        invalid("no gender", s -> s.setGender(null), INVALID_GENDER_610),
        invalid("unknown gender", s -> s.setGender("X"), INVALID_GENDER_VALUE_611),
        invalid("zero passed years", s -> s.setNbrPassedYears(0),
            INVALID_NUMBER_OF_PASSED_YEARS_612),
        invalid("malformed IBAN", s -> s.setIban("BE68-5390"), INVALID_IBAN_613),
        invalid("card holder too long", s -> s.setCardHolder("x".repeat(36)),
            CARD_HOLDER_MAX_LENGTH_OVERFLOW_614),
        invalid("no bank name", s -> s.setBankName(null), INVALID_BANK_NAME_615),
        invalid("bank name too long", s -> s.setBankName("b".repeat(61)),
            BANK_NAME_MAX_LENGTH_OVERFLOW_616),
        invalid("malformed BIC", s -> s.setBic("GKCC"), INVALID_BIC_617));
  }

  private static Arguments invalid(String name, Consumer<NominatedStudent> change, int code) {
    return arguments(named(name, change), code);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("invalidFields")
  void eachInvalidFieldIsReportedWithItsOwnCode(Consumer<NominatedStudent> change, int code) {
    change.accept(student);

    assertThat(Violations.of(student::checkDataIntegrity)).containsExactly(code);
  }

  @Test
  void everyViolationIsReportedAtOnceInFieldOrder() {
    NominatedStudent empty = (NominatedStudent) factory.build(NominatedStudent.class);

    assertThat(Violations.of(empty::checkDataIntegrity)).containsExactly(INVALID_TITLE_601,
        INVALID_BIRTHDATE_NULL_604, INVALID_NATIONALITY_NULL_606, INVALID_ADDRESS_NULL_608,
        INVALID_PHONE_NUMBER_619, INVALID_GENDER_610, INVALID_NUMBER_OF_PASSED_YEARS_612,
        INVALID_IBAN_613, INVALID_BANK_NAME_615, INVALID_BIC_617);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Mr", "M", "Mrs", "Ms"})
  void acceptedTitles(String title) {
    student.setTitle(title);
    assertThat(Violations.of(student::checkDataIntegrity)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"M", "F", "O"})
  void acceptedGenders(String gender) {
    student.setGender(gender);
    assertThat(Violations.of(student::checkDataIntegrity)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void aMissingCardHolderDefaultsToTheStudentName(String cardHolder) {
    student.setCardHolder(cardHolder);

    assertThat(Violations.of(student::checkDataIntegrity)).isEmpty();
    assertThat(student.getCardHolder()).isEqualTo("Alice Martin");
  }

  @Test
  void checkBankDetailsPassesWhenIbanBankAndBicAreSet() {
    assertThat(Violations.of(student::checkBankDetails)).isEmpty();
  }

  static Stream<Arguments> incompleteBankDetails() {
    return Stream.of(
        arguments(named("no IBAN", (Consumer<NominatedStudent>) s -> s.setIban(null))),
        arguments(named("empty bank name", (Consumer<NominatedStudent>) s -> s.setBankName(""))),
        arguments(named("no BIC", (Consumer<NominatedStudent>) s -> s.setBic(null))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("incompleteBankDetails")
  void checkBankDetailsRejectsIncompleteDetails(Consumer<NominatedStudent> change) {
    change.accept(student);

    assertThat(Violations.errorCodeOf(student::checkBankDetails))
        .isEqualTo(INCOMPLETE_BANK_DETAILS_506);
  }

  @Test
  void theUserPartIsNotValidatedByTheStudentCheck() {
    student.setEmail("not an email");
    student.setUsername(null);

    assertThat(Violations.of(student::checkDataIntegrity)).isEqualTo(List.of());
  }
}
