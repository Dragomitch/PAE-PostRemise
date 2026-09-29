package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.EMAIL_MAX_LENGTH_OVERFLOW_215;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.FIRST_NAME_MAX_LENGTH_OVERFLOW_211;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_EMAIL_206;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_FIRST_NAME_201;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_LAST_NAME_202;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_OPTION_208;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_OPTION_CODE_209;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_PASSWORD_205;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_USERNAME_203;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.LAST_NAME_MAX_LENGTH_OVERFLOW_212;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.PASSWORD_MAX_LENGTH_OVERFLOW_214;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.USERNAME_MAX_LENGTH_OVERFLOW_213;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Validation rules of a user, including the maximum lengths of the database columns. */
class UserImplTest {

  private final EntityFactoryImpl factory = new EntityFactoryImpl();
  private User user;

  @BeforeEach
  void validUser() {
    user = (User) factory.build(User.class);
    user.setUsername("alice");
    user.setLastName("Martin");
    user.setFirstName("Alice");
    user.setPassword("secret");
    user.setEmail("alice@student.test");
    OptionDto option = (OptionDto) factory.build(OptionDto.class);
    option.setCode("BIN");
    user.setOption(option);
  }

  @Test
  void aCompleteUserIsValid() {
    assertThat(Violations.of(user::checkDataIntegrity)).isEmpty();
  }

  @Test
  void valuesExactlyAtTheColumnLimitsAreAccepted() {
    user.setUsername("u".repeat(20));
    user.setLastName("l".repeat(35));
    user.setFirstName("f".repeat(35));
    user.setPassword("p".repeat(255));
    user.setEmail("e".repeat(242) + "@example.test");

    assertThat(user.getEmail()).hasSize(255);
    assertThat(Violations.of(user::checkDataIntegrity)).isEmpty();
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        invalid("no username", u -> u.setUsername(null), INVALID_USERNAME_203),
        invalid("username too long", u -> u.setUsername("u".repeat(21)),
            USERNAME_MAX_LENGTH_OVERFLOW_213),
        invalid("empty last name", u -> u.setLastName(""), INVALID_LAST_NAME_202),
        invalid("last name too long", u -> u.setLastName("l".repeat(36)),
            LAST_NAME_MAX_LENGTH_OVERFLOW_212),
        invalid("no first name", u -> u.setFirstName(null), INVALID_FIRST_NAME_201),
        invalid("first name too long", u -> u.setFirstName("f".repeat(36)),
            FIRST_NAME_MAX_LENGTH_OVERFLOW_211),
        invalid("no password", u -> u.setPassword(""), INVALID_PASSWORD_205),
        invalid("password too long", u -> u.setPassword("p".repeat(256)),
            PASSWORD_MAX_LENGTH_OVERFLOW_214),
        invalid("malformed email", u -> u.setEmail("alice.student.test"), INVALID_EMAIL_206),
        invalid("email too long", u -> u.setEmail("e".repeat(243) + "@example.test"),
            EMAIL_MAX_LENGTH_OVERFLOW_215),
        invalid("no option", u -> u.setOption(null), INVALID_OPTION_208),
        invalid("option without code", u -> u.getOption().setCode(""),
            INVALID_OPTION_CODE_209));
  }

  private static Arguments invalid(String name, Consumer<User> change, int code) {
    return arguments(named(name, change), code);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("invalidFields")
  void eachInvalidFieldIsReportedWithItsOwnCode(Consumer<User> change, int code) {
    change.accept(user);

    assertThat(Violations.of(user::checkDataIntegrity)).containsExactly(code);
  }

  @Test
  void everyViolationIsReportedAtOnce() {
    User empty = (User) factory.build(User.class);

    assertThat(Violations.of(empty::checkDataIntegrity)).containsExactly(INVALID_USERNAME_203,
        INVALID_LAST_NAME_202, INVALID_FIRST_NAME_201, INVALID_PASSWORD_205, INVALID_EMAIL_206,
        INVALID_OPTION_208);
  }
}
