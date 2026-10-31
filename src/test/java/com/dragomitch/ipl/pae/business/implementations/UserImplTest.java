package com.dragomitch.ipl.pae.business.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Constraints of a user (formerly {@code checkDataIntegrity()}, error codes 201 to 216), including
 * the maximum lengths of the database columns. A sign-up validates the {@code Default} and
 * {@code OnCreate} groups, an edition the {@code Default} group only.
 */
class UserImplTest {

  /** An email address of exactly 255 characters (64 for the local part, the RFC maximum). */
  static final String LONGEST_EMAIL =
      "e".repeat(64) + "@" + "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(57) + ".test";

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
    assertThat(Violations.onCreate(user)).isEmpty();
  }

  @Test
  void valuesExactlyAtTheColumnLimitsAreAccepted() {
    user.setUsername("u".repeat(20));
    user.setLastName("l".repeat(35));
    user.setFirstName("f".repeat(35));
    user.setPassword("p".repeat(255));
    user.setEmail(LONGEST_EMAIL);

    assertThat(user.getEmail()).hasSize(255);
    assertThat(Violations.onCreate(user)).isEmpty();
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        invalid("no username (203)", u -> u.setUsername(null), "username:NotBlank"),
        invalid("blank username (203)", u -> u.setUsername("  "), "username:NotBlank"),
        invalid("username too long (213)", u -> u.setUsername("u".repeat(21)), "username:Size"),
        invalid("empty last name (202)", u -> u.setLastName(""), "lastName:NotBlank"),
        invalid("last name too long (212)", u -> u.setLastName("l".repeat(36)), "lastName:Size"),
        invalid("no first name (201)", u -> u.setFirstName(null), "firstName:NotBlank"),
        invalid("first name too long (211)", u -> u.setFirstName("f".repeat(36)),
            "firstName:Size"),
        invalid("no password (205)", u -> u.setPassword(""), "password:NotBlank"),
        invalid("password too long (214)", u -> u.setPassword("p".repeat(256)),
            "password:Size"),
        invalid("no email (206)", u -> u.setEmail(null), "email:NotBlank"),
        invalid("malformed email (206)", u -> u.setEmail("alice.student.test"), "email:Email"),
        invalid("email without top-level domain (206)", u -> u.setEmail("alice@localhost"),
            "email:Email"),
        invalid("email too long (215)", u -> u.setEmail("x" + LONGEST_EMAIL), "email:Email",
            "email:Size"),
        invalid("no option (208)", u -> u.setOption(null), "option:NotNull"),
        invalid("option without code (209)", u -> u.getOption().setCode(null),
            "option.code:NotBlank"),
        invalid("option code of 2 characters (216)", u -> u.getOption().setCode("BI"),
            "option.code:Size"));
  }

  private static Arguments invalid(String name, Consumer<User> change, String... violations) {
    return arguments(named(name, change), violations);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("invalidFields")
  void eachInvalidFieldIsReportedOnItsProperty(Consumer<User> change, String[] violations) {
    change.accept(user);

    assertThat(Violations.onCreate(user)).containsExactly(violations);
  }

  @Test
  void everyViolationIsReportedAtOnce() {
    User empty = (User) factory.build(User.class);

    assertThat(Violations.onCreate(empty)).containsExactly("email:NotBlank",
        "firstName:NotBlank", "lastName:NotBlank", "option:NotNull", "password:NotBlank",
        "username:NotBlank");
  }

  @Test
  void anEditionDoesNotNeedThePassword() {
    // the stored password is kept by UserUcc.edit
    user.setPassword(null);

    assertThat(Violations.of(user)).isEmpty();
    assertThat(Violations.onCreate(user)).containsExactly("password:NotBlank");
  }

  @ParameterizedTest
  @ValueSource(strings = {"alice@student.test", "a.b-c@mail.example.org", "a+tag@example.org", "x_y@d-1.be"})
  void acceptedEmailAddresses(String email) {
    user.setEmail(email);

    assertThat(Violations.of(user)).isEmpty();
  }

  @Test
  void theOptionIsOnlyReferencedByItsCode() {
    // the name of the option is not required: only the code identifies it
    user.getOption().setName(null);

    assertThat(Violations.of(user)).isEmpty();
  }
}
