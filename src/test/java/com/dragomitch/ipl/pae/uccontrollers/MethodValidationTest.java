package com.dragomitch.ipl.pae.uccontrollers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.persistence.UserDao;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

/**
 * The use cases validate their arguments themselves ({@code @Validated} interfaces with
 * constraints on the parameters), whoever calls them: the rules hold outside the web layer. The
 * messages of the violations come from the application's {@code MessageSource}.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class MethodValidationTest {

  @Autowired
  private ApplicationContext context;
  @Autowired
  private EntityFactory entityFactory;
  @Autowired
  private MessageSource messageSource;
  @Autowired
  private UserUcc userUcc;
  @Autowired
  private UserDao userDao;
  @Autowired
  private PartnerUcc partnerUcc;
  @Autowired
  private MobilityUcc mobilityUcc;
  @Autowired
  private MobilityChoiceUcc mobilityChoiceUcc;
  @Autowired
  private NominatedStudentUcc nominatedStudentUcc;
  @Autowired
  private ProgrammeUcc programmeUcc;
  @Autowired
  private CountryUcc countryUcc;
  @Autowired
  private OptionUcc optionUcc;
  @Autowired
  private SessionUcc sessionUcc;
  @Autowired
  private PaymentUcc paymentUcc;

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  static Stream<Class<?>> useCases() {
    return Stream.of(AddressUcc.class, CountryUcc.class, DenialReasonUcc.class,
        MobilityChoiceUcc.class, MobilityUcc.class, NominatedStudentUcc.class, OptionUcc.class,
        PartnerUcc.class, PaymentUcc.class, ProgrammeUcc.class, SessionUcc.class, UserUcc.class);
  }

  @ParameterizedTest
  @MethodSource("useCases")
  void everyUseCaseIsValidatedByAProxy(Class<?> useCase) {
    Object bean = context.getBean(useCase);

    assertThat(bean).isInstanceOf(Advised.class);
    assertThat(Arrays.stream(((Advised) bean).getAdvisors()).map(a -> a.getAdvice()))
        .anyMatch(MethodValidationInterceptor.class::isInstance);
  }

  @Test
  void anInvalidSignupIsRefusedBeforeReachingTheDatabase() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setUsername("u".repeat(21));
    user.setEmail("not an email");

    assertThat(Violations.thrownBy(() -> userUcc.signup(user))).containsExactly(
        "signup.user.email:Email", "signup.user.firstName:NotBlank",
        "signup.user.lastName:NotBlank", "signup.user.option:NotNull",
        "signup.user.password:NotBlank", "signup.user.username:Size");
    assertThat(userDao.findAll()).isEmpty();
  }

  @Test
  void anEditionDoesNotRequireThePassword() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);

    assertThat(Violations.thrownBy(() -> userUcc.edit(user, 1, UserDto.ROLE_STUDENT)))
        .doesNotContain("edit.user.password:NotBlank")
        .contains("edit.user.username:NotBlank");
  }

  static Stream<Arguments> invalidCalls() {
    return Stream.of(
        Arguments.of("promoteToProfessor.userId:Positive",
            (Call) t -> t.userUcc.promoteToProfessor(0)),
        Arguments.of("signup.user:NotNull", (Call) t -> t.userUcc.signup(null)),
        Arguments.of("signin.password:NotBlank", (Call) t -> t.sessionUcc.signin("jdoe", " ")),
        Arguments.of("showAuthenticatedUser.id:Positive",
            (Call) t -> t.sessionUcc.showAuthenticatedUser(-3)),
        Arguments.of("showOne.id:Positive", (Call) t -> t.partnerUcc.showOne(0)),
        Arguments.of("showAll.search.filter:Pattern",
            (Call) t -> t.partnerUcc.showAll(new PartnerSearch("name", "x"), "Student", 1)),
        Arguments.of("showAll.search.value:FilterValueRequired",
            (Call) t -> t.partnerUcc.showAll(new PartnerSearch("country", null), "Student", 1)),
        Arguments.of("showOne.id:Positive",
            (Call) t -> t.mobilityUcc.showOne(0, UserDto.ROLE_PROFESSOR, 1)),
        Arguments.of("confirmDocument.document:Positive",
            (Call) t -> t.mobilityUcc.confirmDocument(1, -1, 1)),
        Arguments.of("exportDocuments.filter:Pattern",
            (Call) t -> t.mobilityUcc.exportDocuments(1, "X")),
        Arguments.of("cancel.cancellationReason:Size", (Call) t -> t.mobilityUcc.cancel(1, 1,
            "x".repeat(501), 0, 1, UserDto.ROLE_STUDENT)),
        Arguments.of("cancel.reason:NotBlank", (Call) t -> t.mobilityChoiceUcc.cancel(1, 1, "")),
        Arguments.of("reject.reasonId:Positive", (Call) t -> t.mobilityChoiceUcc.reject(1, 0)),
        Arguments.of("showAll.filter:Pattern",
            (Call) t -> t.mobilityChoiceUcc.showAll(1, UserDto.ROLE_STUDENT, "mine")),
        Arguments.of("showOne.id:Positive",
            (Call) t -> t.nominatedStudentUcc.showOne(0, 1, UserDto.ROLE_PROFESSOR)),
        Arguments.of("showOne.id:Positive", (Call) t -> t.programmeUcc.showOne(-1)),
        Arguments.of("showOne.countryCode:Size", (Call) t -> t.countryUcc.showOne("BEL")),
        Arguments.of("findAllPartnersByOption.optionCode:Size",
            (Call) t -> t.optionUcc.findAllPartnersByOption("BINF")),
        Arguments.of("showAll.userRole:NotBlank", (Call) t -> t.paymentUcc.showAll("")));
  }

  /** A call of a use case of this test. */
  interface Call {
    void call(MethodValidationTest test) throws Throwable;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidCalls")
  void invalidArgumentsAreRefusedByTheUseCases(String violation, Call call) {
    ThrowingCallable callable = () -> call.call(this);

    assertThat(Violations.thrownBy(callable)).containsExactly(violation);
  }

  @Test
  void theMessagesComeFromTheApplicationBundleInTheLanguageOfTheRequest() {
    NominatedStudentDto student =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);

    LocaleContextHolder.setLocale(Locale.ENGLISH);
    Map<String, String> english = messages(() -> nominatedStudentUcc.create(student, 1,
        UserDto.ROLE_PROFESSOR));
    LocaleContextHolder.setLocale(Locale.FRENCH);
    Map<String, String> french = messages(() -> nominatedStudentUcc.create(student, 1,
        UserDto.ROLE_PROFESSOR));

    assertThat(english).containsEntry("create.nominatedStudent.title", "must not be blank")
        .containsEntry("create.nominatedStudent.address", "is required")
        .containsEntry("create.nominatedStudent.nbrPassedYears", "must be greater than 0");
    assertThat(french).containsEntry("create.nominatedStudent.title", "ne peut pas être vide")
        .containsEntry("create.nominatedStudent.address", "est obligatoire")
        .containsEntry("create.nominatedStudent.nbrPassedYears", "doit être strictement positif");
  }

  @Test
  void theMessagesOfTheCustomConstraintsAreTranslatedToo() {
    NominatedStudentDto student =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    student.setIban("BE00");
    student.setBic("B093");

    LocaleContextHolder.setLocale(Locale.ENGLISH);
    Map<String, String> english = messages(() -> nominatedStudentUcc.edit(student, 1,
        UserDto.ROLE_PROFESSOR));

    assertThat(english)
        .containsEntry("edit.nominatedStudent.iban",
            "must be a valid IBAN (for example BE68539007547034, without spaces)")
        .containsEntry("edit.nominatedStudent.bic",
            "must be a valid BIC code of 8 or 11 characters (for example GEBABEBB)");
  }

  private static Map<String, String> messages(ThrowingCallable call) {
    try {
      call.call();
    } catch (ConstraintViolationException ex) {
      return ex.getConstraintViolations().stream().collect(Collectors.toMap(
          v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a));
    } catch (Throwable ex) {
      throw new AssertionError("Expected a ConstraintViolationException", ex);
    }
    throw new AssertionError("Expected a ConstraintViolationException");
  }

  @Test
  void theProblemMessagesAreResolvedWithTheirArguments() {
    ErrorCode code = ErrorCode.EMAIL_TAKEN;

    assertThat(messageSource.getMessage(code.detailMessageCode(), new Object[] {"a@b.be"},
        Locale.ENGLISH)).isEqualTo("The email address “a@b.be” is already associated with an "
        + "account.");
    assertThat(messageSource.getMessage(code.detailMessageCode(), new Object[] {"a@b.be"},
        Locale.FRENCH)).isEqualTo("L’adresse e-mail « a@b.be » est déjà associée à un compte.");
  }

  @Test
  void anotherLanguageFallsBackOnFrenchNotOnTheLanguageOfTheServer() {
    // the test JVM runs in English (-Duser.language=en): with fallback-to-system-locale=true a
    // German request would get English messages
    assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");

    assertThat(messageSource.getMessage(ErrorCode.ACCESS_DENIED.titleMessageCode(), null,
        Locale.GERMAN)).isEqualTo("Accès refusé");
  }

  @Test
  void theBusinessRulesStayInTheUseCases() {
    // uniqueness needs the database: it is not a constraint but a business error
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setUsername("jdoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("jdoe@example.test");
    user.setPassword("secret");
    user.setOption(new MockDtoFactory(entityFactory).getOption());
    userUcc.signup(user);

    UserDto sameUsername = new MockDtoFactory(entityFactory).getUser(UserDto.ROLE_STUDENT);
    sameUsername.setUsername("jdoe");
    assertThat(Violations.errorCodeOf(() -> userUcc.signup(sameUsername)))
        .isEqualTo(ErrorCode.USERNAME_TAKEN);
    UserDto sameEmail = new MockDtoFactory(entityFactory).getUser(UserDto.ROLE_STUDENT);
    sameEmail.setEmail("jdoe@example.test");
    assertThat(Violations.errorCodeOf(() -> userUcc.signup(sameEmail)))
        .isEqualTo(ErrorCode.EMAIL_TAKEN);
    UserDto unknownOption = new MockDtoFactory(entityFactory).getUser(UserDto.ROLE_STUDENT);
    unknownOption.getOption().setCode("ZZZ");
    assertThat(Violations.errorCodeOf(() -> userUcc.signup(unknownOption)))
        .isEqualTo(ErrorCode.UNKNOWN_OPTION);
    assertThatThrownBy(() -> userUcc.signup(null))
        .isInstanceOf(ConstraintViolationException.class);
  }
}
