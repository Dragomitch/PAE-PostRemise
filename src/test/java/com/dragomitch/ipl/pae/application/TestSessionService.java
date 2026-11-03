package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.SessionService;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Sign-in against the accounts registered by {@link UserService#signup}: the password is checked
 * against its BCrypt hash, and a wrong username or password gives the same error. The mock DAOs are
 * emptied before each test by {@code InMemoryRepositoryResetListener}.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class TestSessionService {

  private static final String PASSWORD = "S3cret-passw0rd";

  @Autowired private SessionService sessionService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;

  private User registered;

  @BeforeEach
  void registerAUser() {
    User user = new MockDtoFactory().getUser(Role.STUDENT);
    user.setUsername("alice");
    user.setEmail("alice@student.test");
    user.setPassword(PASSWORD);
    registered = userService.signup(user);
  }

  @Test
  void theRightCredentialsGiveTheUser() {
    User signedIn = sessionService.signin("alice", PASSWORD);

    assertThat(signedIn.getId()).isEqualTo(registered.getId());
    assertThat(signedIn.getUsername()).isEqualTo("alice");
    assertThat(signedIn.getRole()).isEqualTo(Role.PROFESSOR);
    // only the hash is stored
    assertThat(userRepository.findById(registered.getId()).getPassword())
        .isNotEqualTo(PASSWORD)
        .startsWith("$2");
  }

  @ParameterizedTest
  @CsvSource({"alice, s3cret-passw0rd", "alice, 'S3cret-passw0rd '", "bob, S3cret-passw0rd"})
  void wrongCredentialsAreRejectedWithoutTellingWhichPartIsWrong(String username, String password) {
    assertThatBusinessException(() -> sessionService.signin(username, password))
        .hasErrorCode(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  void theAuthenticatedUserIsReadByTheIdOfTheSession() {
    assertThat(sessionService.showAuthenticatedUser(registered.getId()).getUsername())
        .isEqualTo("alice");
  }

  @Test
  void anUnknownIdHasNoUser() {
    assertThat(sessionService.showAuthenticatedUser(registered.getId() + 1)).isNull();
  }
}
