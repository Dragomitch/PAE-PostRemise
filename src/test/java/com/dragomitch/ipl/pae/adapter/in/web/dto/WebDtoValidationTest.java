package com.dragomitch.ipl.pae.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Violations;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * The constraints of the web DTOs, validated by the application's validator: their messages come
 * from the application's {@code MessageSource}, in the language of the request.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class WebDtoValidationTest {

  @Autowired private Validator validator;
  @Autowired private MessageSource messageSource;

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  private static UserDto user(String username, String email, String password) {
    return new UserDto(0, null, null, username, password, email, null, null, null, 0);
  }

  @Test
  void anInvalidSignupBreaksTheConstraintsOfTheUserAndOfTheCreation() {
    assertThat(
            Violations.of(
                user("u".repeat(21), "not an email", null), Default.class, OnCreate.class))
        .containsExactly(
            "email:Email",
            "firstName:NotBlank",
            "lastName:NotBlank",
            "option:NotNull",
            "password:NotBlank",
            "username:Size");
  }

  @Test
  void anEditionDoesNotRequireThePassword() {
    assertThat(Violations.of(user(null, null, null)))
        .doesNotContain("password:NotBlank")
        .contains("username:NotBlank");
  }

  @ParameterizedTest(name = "mobility type \"{0}\": {1}")
  @CsvSource({"'', NotBlank", "SMT, Pattern", "sms, Pattern"})
  void theMobilityTypeIsSmsOrSmp(String type, String constraint) {
    MobilityChoiceDto choice =
        new MobilityChoiceDto(
            0,
            new UserDto(1, null, null, null, null, null, null, null, null, 0),
            1,
            type,
            2026,
            1,
            new ProgrammeDto(1, null, null),
            null,
            null,
            null,
            null,
            null,
            0);

    assertThat(Violations.of(choice)).containsExactly("mobilityType:" + constraint);
    assertThat(
            Violations.of(
                new MobilityChoiceDto(
                    0,
                    choice.user(),
                    1,
                    "SMP",
                    2026,
                    1,
                    choice.programme(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    0)))
        .isEmpty();
  }

  @Test
  void theMessagesComeFromTheApplicationBundleInTheLanguageOfTheRequest() {
    NominatedStudentDto student = emptyStudent(null, null);

    LocaleContextHolder.setLocale(Locale.ENGLISH);
    Map<String, String> english = messages(student);
    LocaleContextHolder.setLocale(Locale.FRENCH);
    Map<String, String> french = messages(student);

    assertThat(english)
        .containsEntry("title", "must not be blank")
        .containsEntry("address", "is required")
        .containsEntry("nbrPassedYears", "must be greater than 0");
    assertThat(french)
        .containsEntry("title", "ne peut pas être vide")
        .containsEntry("address", "est obligatoire")
        .containsEntry("nbrPassedYears", "doit être strictement positif");
  }

  @Test
  void theMessagesOfTheCustomConstraintsAreTranslatedToo() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
    Map<String, String> english = messages(emptyStudent("BE00", "B093"));

    assertThat(english)
        .containsEntry(
            "iban", "must be a valid IBAN (for example BE68539007547034, without spaces)")
        .containsEntry(
            "bic", "must be a valid BIC code of 8 or 11 characters (for example GEBABEBB)");
  }

  @Test
  void theProblemMessagesAreResolvedWithTheirArguments() {
    ErrorCode code = ErrorCode.EMAIL_TAKEN;

    assertThat(
            messageSource.getMessage(
                code.detailMessageCode(), new Object[] {"a@b.be"}, Locale.ENGLISH))
        .isEqualTo("The email address “a@b.be” is already associated with an account.");
    assertThat(
            messageSource.getMessage(
                code.detailMessageCode(), new Object[] {"a@b.be"}, Locale.FRENCH))
        .isEqualTo("L’adresse e-mail « a@b.be » est déjà associée à un compte.");
  }

  @Test
  void anotherLanguageFallsBackOnFrenchNotOnTheLanguageOfTheServer() {
    // the test JVM runs in English (-Duser.language=en): with fallback-to-system-locale=true a
    // German request would get English messages
    assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");

    assertThat(
            messageSource.getMessage(
                ErrorCode.ACCESS_DENIED.titleMessageCode(), null, Locale.GERMAN))
        .isEqualTo("Accès refusé");
  }

  private static NominatedStudentDto emptyStudent(String iban, String bic) {
    return new NominatedStudentDto(
        0, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0,
        iban, null, null, bic, 0);
  }

  private Map<String, String> messages(Object bean) {
    return validator.validate(bean).stream()
        .collect(
            Collectors.toMap(
                v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a));
  }
}
