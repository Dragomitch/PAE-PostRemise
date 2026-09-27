package com.dragomitch.ipl.pae.uccontrollers;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.persistence.UserDao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Sign-in against the accounts registered by {@link UserUcc#signup}: the password is checked
 * against its BCrypt hash, and a wrong username or password gives the same error. The mock DAOs
 * are emptied before each test by {@code MockDaoResetListener}.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class TestSessionUcc {

  private static final String PASSWORD = "S3cret-passw0rd";

  @Autowired
  private EntityFactory entityFactory;
  @Autowired
  private SessionUcc sessionUcc;
  @Autowired
  private UserUcc userUcc;
  @Autowired
  private UserDao userDao;

  private UserDto registered;

  @BeforeEach
  void registerAUser() {
    UserDto user = new MockDtoFactory(entityFactory).getUser(UserDto.ROLE_STUDENT);
    user.setUsername("alice");
    user.setEmail("alice@student.test");
    user.setPassword(PASSWORD);
    registered = userUcc.signup(user);
  }

  @Test
  void theRightCredentialsGiveTheUser() {
    UserDto signedIn = sessionUcc.signin("alice", PASSWORD);

    assertThat(signedIn.getId()).isEqualTo(registered.getId());
    assertThat(signedIn.getUsername()).isEqualTo("alice");
    assertThat(signedIn.getRole()).isEqualTo(UserDto.ROLE_PROFESSOR);
    // only the hash is stored
    assertThat(userDao.findById(registered.getId()).getPassword()).isNotEqualTo(PASSWORD)
        .startsWith("$2");
  }

  @ParameterizedTest
  @CsvSource({"alice, s3cret-passw0rd", "alice, 'S3cret-passw0rd '", "bob, S3cret-passw0rd"})
  void wrongCredentialsAreRejectedWithoutTellingWhichPartIsWrong(String username,
      String password) {
    assertThatBusinessException(() -> sessionUcc.signin(username, password))
        .hasErrorCode(ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  void theAuthenticatedUserIsReadByTheIdOfTheSession() {
    assertThat(sessionUcc.showAuthenticatedUser(registered.getId()).getUsername())
        .isEqualTo("alice");
  }

  @Test
  void anUnknownIdHasNoUser() {
    assertThat(sessionUcc.showAuthenticatedUser(registered.getId() + 1)).isNull();
  }
}
