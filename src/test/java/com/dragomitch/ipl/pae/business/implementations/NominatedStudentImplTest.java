package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.ErrorCode.INCOMPLETE_BANK_DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Constraints of a nominated student (formerly {@code checkDataIntegrity()}, error codes 601 to
 * 619), one field at a time.
 */
class NominatedStudentImplTest {

  private final EntityFactoryImpl factory = new EntityFactoryImpl();
  private NominatedStudent student;

  private CountryDto country(String code) {
    CountryDto country = (CountryDto) factory.build(CountryDto.class);
    country.setCountryCode(code);
    return country;
  }

  @BeforeEach
  void validStudent() {
    student = (NominatedStudent) factory.build(NominatedStudent.class);
    student.setFirstName("Alice");
    student.setLastName("Martin");
    student.setUsername("alice");
    student.setEmail("alice@student.test");
    OptionDto option = (OptionDto) factory.build(OptionDto.class);
    option.setCode("BIN");
    student.setOption(option);
    student.setTitle("Ms");
    student.setBirthdate(LocalDate.of(2001, 4, 12));
    student.setNationality(country("BE"));
    AddressDto address = (AddressDto) factory.build(AddressDto.class);
    address.setStreet("Rue de la Loi");
    address.setNumber("16");
    address.setCity("Bruxelles");
    address.setPostalCode("1000");
    address.setRegion("");
    address.setCountry(country("BE"));
    student.setAddress(address);
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
    assertThat(Violations.of(student)).isEmpty();
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        invalid("no title (601)", s -> s.setTitle(null), "title:NotBlank"),
        invalid("empty title (601)", s -> s.setTitle(""), "title:NotBlank"),
        invalid("title too long (603)", s -> s.setTitle("Miss"), "title:Pattern"),
        invalid("unknown title (602)", s -> s.setTitle("Dr"), "title:Pattern"),
        invalid("no birthdate (604)", s -> s.setBirthdate(null), "birthdate:NotNull"),
        invalid("born today (605)", s -> s.setBirthdate(LocalDate.now()), "birthdate:Past"),
        invalid("born tomorrow (605)", s -> s.setBirthdate(LocalDate.now().plusDays(1)),
            "birthdate:Past"),
        invalid("no nationality (606)", s -> s.setNationality(null), "nationality:NotNull"),
        invalid("nationality without code (900)", s -> s.getNationality().setCountryCode(null),
            "nationality.countryCode:NotBlank"),
        invalid("nationality code of 3 letters (314)",
            s -> s.getNationality().setCountryCode("BEL"), "nationality.countryCode:Size"),
        invalid("no address (608)", s -> s.setAddress(null), "address:NotNull"),
        invalid("invalid address", s -> s.getAddress().setCity(""), "address.city:NotBlank"),
        invalid("no phone number (619)", s -> s.setPhoneNumber(""), "phoneNumber:NotBlank"),
        invalid("phone number too long (609)", s -> s.setPhoneNumber("+3247000000000001"),
            "phoneNumber:Size"),
        invalid("malformed phone number (619)", s -> s.setPhoneNumber("call me"),
            "phoneNumber:PhoneNumber"),
        invalid("no gender (610)", s -> s.setGender(null), "gender:NotBlank"),
        invalid("unknown gender (611)", s -> s.setGender("X"), "gender:Pattern"),
        invalid("zero passed years (612)", s -> s.setNbrPassedYears(0),
            "nbrPassedYears:Positive"),
        invalid("no IBAN (613)", s -> s.setIban(null), "iban:NotBlank"),
        invalid("malformed IBAN (613)", s -> s.setIban("BE68-5390"), "iban:Iban"),
        invalid("IBAN with a wrong checksum (613)", s -> s.setIban("BE68539007547035"),
            "iban:Iban"),
        invalid("IBAN longer than the column (613)", s -> s.setIban("LC55HEMM000100010012001200023015"),
            "iban:Size"),
        invalid("card holder too long (614)", s -> s.setCardHolder("x".repeat(36)),
            "cardHolder:Size"),
        invalid("no bank name (615)", s -> s.setBankName(null), "bankName:NotBlank"),
        invalid("bank name too long (616)", s -> s.setBankName("b".repeat(61)),
            "bankName:Size"),
        invalid("no BIC (617)", s -> s.setBic(""), "bic:NotBlank"),
        invalid("malformed BIC (617)", s -> s.setBic("GKCC"), "bic:Bic"),
        invalid("user part: malformed email", s -> s.setEmail("not an email"), "email:Email"),
        invalid("user part: no username", s -> s.setUsername(null), "username:NotBlank"));
  }

  private static Arguments invalid(String name, Consumer<NominatedStudent> change,
      String violation) {
    return arguments(named(name, change), violation);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("invalidFields")
  void eachInvalidFieldIsReportedOnItsProperty(Consumer<NominatedStudent> change,
      String violation) {
    change.accept(student);

    assertThat(Violations.of(student)).containsExactly(violation);
  }

  @Test
  void everyViolationIsReportedAtOnce() {
    NominatedStudent empty = (NominatedStudent) factory.build(NominatedStudent.class);

    assertThat(Violations.of(empty)).containsExactly("address:NotNull", "bankName:NotBlank",
        "bic:NotBlank", "birthdate:NotNull", "email:NotBlank", "firstName:NotBlank",
        "gender:NotBlank", "iban:NotBlank", "lastName:NotBlank", "nationality:NotNull",
        "nbrPassedYears:Positive", "option:NotNull", "phoneNumber:NotBlank", "title:NotBlank",
        "username:NotBlank");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Mr", "M", "Mrs", "Ms"})
  void acceptedTitles(String title) {
    student.setTitle(title);
    assertThat(Violations.of(student)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"M", "F", "O"})
  void acceptedGenders(String gender) {
    student.setGender(gender);
    assertThat(Violations.of(student)).isEmpty();
  }

  @Test
  void theCardHolderIsOptional() {
    // NominatedStudentUcc then uses the name of the student
    student.setCardHolder(null);

    assertThat(Violations.of(student)).isEmpty();
  }

  @Test
  void thePasswordIsNotRequired() {
    // the personal data are sent without the password of the account
    student.setPassword(null);

    assertThat(Violations.of(student)).isEmpty();
  }

  @Test
  void checkBankDetailsPassesWhenIbanBankAndBicAreSet() {
    assertThatCode(student::checkBankDetails).doesNotThrowAnyException();
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
        .isEqualTo(INCOMPLETE_BANK_DETAILS);
  }
}
