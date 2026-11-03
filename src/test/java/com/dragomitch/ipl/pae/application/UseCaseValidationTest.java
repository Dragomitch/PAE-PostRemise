package com.dragomitch.ipl.pae.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.AddressService;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import com.dragomitch.ipl.pae.domain.port.in.MobilityService;
import com.dragomitch.ipl.pae.domain.port.in.NominatedStudentService;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.in.PaymentService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import com.dragomitch.ipl.pae.domain.port.in.SessionService;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

/**
 * The format of the data is checked by the driving adapter (the constraints of the web DTOs and of
 * the request parameters); the use cases check the rules that need the database or the state. The
 * domain carries no Bean Validation, so no use case is proxied for method validation.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class UseCaseValidationTest {

  @Autowired private ApplicationContext context;
  @Autowired private UserService userService;

  static Stream<Class<?>> useCases() {
    return Stream.of(
        AddressService.class,
        CountryService.class,
        DenialReasonService.class,
        MobilityChoiceService.class,
        MobilityService.class,
        NominatedStudentService.class,
        OptionService.class,
        PartnerService.class,
        PaymentService.class,
        ProgrammeService.class,
        SessionService.class,
        UserService.class);
  }

  @ParameterizedTest
  @MethodSource("useCases")
  void noUseCaseIsValidatedByAProxy(Class<?> useCase) {
    Object bean = context.getBean(useCase);

    if (bean instanceof Advised advised) {
      assertThat(Arrays.stream(advised.getAdvisors()).map(a -> a.getAdvice()))
          .noneMatch(MethodValidationInterceptor.class::isInstance);
    }
  }

  @Test
  void theBusinessRulesStayInTheUseCases() {
    // uniqueness needs the database: it is not a constraint but a business error
    User user = new User();
    user.setUsername("jdoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("jdoe@example.test");
    user.setPassword("secret");
    user.setOption(new MockDtoFactory().getOption());
    userService.signup(user);

    User sameUsername = new MockDtoFactory().getUser(Role.STUDENT);
    sameUsername.setUsername("jdoe");
    assertThat(Violations.errorCodeOf(() -> userService.signup(sameUsername)))
        .isEqualTo(ErrorCode.USERNAME_TAKEN);
    User sameEmail = new MockDtoFactory().getUser(Role.STUDENT);
    sameEmail.setEmail("jdoe@example.test");
    assertThat(Violations.errorCodeOf(() -> userService.signup(sameEmail)))
        .isEqualTo(ErrorCode.EMAIL_TAKEN);
    User unknownOption = new MockDtoFactory().getUser(Role.STUDENT);
    unknownOption.getOption().setCode("ZZZ");
    assertThat(Violations.errorCodeOf(() -> userService.signup(unknownOption)))
        .isEqualTo(ErrorCode.UNKNOWN_OPTION);
  }
}
