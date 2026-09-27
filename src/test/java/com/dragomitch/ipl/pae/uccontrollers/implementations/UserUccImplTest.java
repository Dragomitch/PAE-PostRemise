package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.implementations.EntityFactories;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.MockDtoFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * User accounts: registration (first user is a professor), promotion, edition by the owner or a
 * professor, unique username and email, existing option.
 */
@ExtendWith(MockitoExtension.class)
class UserUccImplTest {

  private static final int USER_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final LocalDateTime REGISTERED = LocalDateTime.of(2015, 9, 14, 8, 0);

  @Mock
  private UserDao userDao;
  @Mock
  private OptionDao optionDao;
  @Mock
  private NominatedStudentDao nominatedStudentDao;
  @Mock
  private PasswordEncoder passwordEncoder;

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
  private UserUccImpl userUcc;

  @BeforeEach
  void setUp() {
    userUcc = new UserUccImpl(userDao, optionDao, nominatedStudentDao, passwordEncoder);
  }

  private UserDto user(int id, String role) {
    UserDto user = dtos.getUser(role);
    user.setId(id);
    return user;
  }

  private void givenOptionExists(String code) {
    when(optionDao.findByCode(code)).thenReturn((OptionDto) entityFactory.build(OptionDto.class));
  }

  @Nested
  class Signup {

    private UserDto newUser() {
      UserDto user = user(0, null);
      user.setPassword("plain password");
      user.setRegistrationDate(null);
      return user;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void theFirstUserIsAProfessorTheOthersAreStudents(boolean noUserYet) {
      UserDto user = newUser();
      givenOptionExists("BIN");
      when(userDao.isEmpty()).thenReturn(noUserYet);
      when(passwordEncoder.encode("plain password")).thenReturn("$2a$hash");
      when(userDao.create(user)).thenReturn(user);

      UserDto created = userUcc.signup(user);

      assertThat(created.getRole())
          .isEqualTo(noUserYet ? UserDto.ROLE_PROFESSOR : UserDto.ROLE_STUDENT);
      assertThat(created.getPassword()).isEqualTo("$2a$hash");
      assertThat(created.getRegistrationDate())
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void theUsernameMustBeFree() {
      UserDto user = newUser();
      when(userDao.findBy(UserDao.COLUMN_USERNAME, user.getUsername()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userUcc.signup(user))
          .hasErrorCode(ErrorCode.USERNAME_TAKEN).hasArguments(user.getUsername());
      verify(userDao, never()).create(any());
    }

    @Test
    void theEmailMustBeFree() {
      UserDto user = newUser();
      when(userDao.findBy(UserDao.COLUMN_USERNAME, user.getUsername())).thenReturn(null);
      when(userDao.findBy(UserDao.COLUMN_EMAIL, user.getEmail()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userUcc.signup(user))
          .hasErrorCode(ErrorCode.EMAIL_TAKEN).hasArguments(user.getEmail());
      verify(userDao, never()).create(any());
    }

    @Test
    void theOptionMustExist() {
      UserDto user = newUser();
      user.getOption().setCode("XYZ");

      assertThatBusinessException(() -> userUcc.signup(user))
          .hasErrorCode(ErrorCode.UNKNOWN_OPTION).hasArguments("XYZ");
      verify(userDao, never()).create(any());
      verifyNoInteractions(passwordEncoder);
    }
  }

  @Test
  void showAllListsEveryUser() {
    List<UserDto> all = List.of(user(USER_ID, UserDto.ROLE_STUDENT));
    when(userDao.findAll()).thenReturn(all);

    assertThat(userUcc.showAll()).isSameAs(all);
  }

  @Nested
  class PromoteToProfessor {

    @Test
    void aStudentBecomesAProfessor() {
      UserDto student = user(USER_ID, UserDto.ROLE_STUDENT);
      when(userDao.findById(USER_ID)).thenReturn(student);

      userUcc.promoteToProfessor(USER_ID);

      assertThat(student.getRole()).isEqualTo(UserDto.ROLE_PROFESSOR);
      verify(userDao).update(student);
    }

    @Test
    void promotingAProfessorWritesNothing() {
      when(userDao.findById(PROFESSOR_ID)).thenReturn(user(PROFESSOR_ID, UserDto.ROLE_PROFESSOR));

      userUcc.promoteToProfessor(PROFESSOR_ID);

      verify(userDao, never()).update(any());
    }

    @Test
    void anUnknownUserIsNotFound() {
      assertThatBusinessException(() -> userUcc.promoteToProfessor(USER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(userDao, never()).update(any());
    }
  }

  @Nested
  class Edit {

    /** The account as stored: its password hash, registration date and role. */
    private UserDto givenStoredUser(String role) {
      UserDto stored = user(USER_ID, role);
      stored.setPassword("$2a$stored-hash");
      stored.setRegistrationDate(REGISTERED);
      when(userDao.findById(USER_ID)).thenReturn(stored);
      return stored;
    }

    /** {@link UserDao#update} copies the new version of the row into the DTO. */
    private void givenUpdateIncrementsTheVersion() {
      doAnswer(invocation -> {
        UserDto user = invocation.getArgument(0);
        user.setVersion(user.getVersion() + 1);
        return null;
      }).when(userDao).update(any());
    }

    /** The data sent by the client, trying to change what it may not. */
    private UserDto request() {
      UserDto request = user(USER_ID, UserDto.ROLE_PROFESSOR);
      request.setLastName("Martin-Dupont");
      request.setPassword("new password");
      request.setRegistrationDate(LocalDateTime.now());
      request.setVersion(3);
      return request;
    }

    @ParameterizedTest
    @ValueSource(ints = {USER_ID, PROFESSOR_ID})
    void theOwnerOrAProfessorEditsTheAccountButNotItsPasswordDateOrRole(int requester) {
      String role = requester == USER_ID ? UserDto.ROLE_STUDENT : UserDto.ROLE_PROFESSOR;
      UserDto request = request();
      givenOptionExists("BIN");
      givenStoredUser(UserDto.ROLE_STUDENT);

      UserDto edited = userUcc.edit(request, requester, role);

      assertThat(edited.getLastName()).isEqualTo("Martin-Dupont");
      assertThat(edited.getPassword()).isEqualTo("$2a$stored-hash");
      assertThat(edited.getRegistrationDate()).isEqualTo(REGISTERED);
      assertThat(edited.getRole()).isEqualTo(UserDto.ROLE_STUDENT);
      verify(userDao).update(request);
    }

    @Test
    void theOwnUsernameAndEmailMayBeKept() {
      UserDto request = request();
      when(userDao.findBy(UserDao.COLUMN_USERNAME, request.getUsername()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));
      when(userDao.findBy(UserDao.COLUMN_EMAIL, request.getEmail()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));
      givenOptionExists("BIN");
      givenStoredUser(UserDto.ROLE_STUDENT);

      assertThat(userUcc.edit(request, USER_ID, UserDto.ROLE_STUDENT)).isSameAs(request);
    }

    @Test
    void aStaleNominatedStudentRowIsBroughtToTheVersionOfTheUser() {
      UserDto request = request();
      givenOptionExists("BIN");
      givenStoredUser(UserDto.ROLE_STUDENT);
      givenUpdateIncrementsTheVersion();
      NominatedStudentDto student =
          (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
      student.setId(USER_ID);
      student.setVersion(1);
      when(nominatedStudentDao.findById(USER_ID)).thenReturn(student);

      userUcc.edit(request, USER_ID, UserDto.ROLE_STUDENT);

      verify(nominatedStudentDao).update(student);
    }

    @Test
    void aNominatedStudentRowAtTheVersionOfTheUserIsLeftAlone() {
      UserDto request = request();
      givenOptionExists("BIN");
      givenStoredUser(UserDto.ROLE_STUDENT);
      NominatedStudentDto student =
          (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
      // already at the version the user row gets
      student.setVersion(request.getVersion() + 1);
      givenUpdateIncrementsTheVersion();
      when(nominatedStudentDao.findById(USER_ID)).thenReturn(student);

      userUcc.edit(request, USER_ID, UserDto.ROLE_STUDENT);

      verify(nominatedStudentDao, never()).update(any());
    }

    @Test
    void aStudentCannotEditAnotherAccount() {
      assertThatBusinessException(() -> userUcc.edit(request(), USER_ID + 1,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(userDao, optionDao, nominatedStudentDao);
    }

    @Test
    void theUsernameOfAnotherAccountIsTaken() {
      UserDto request = request();
      when(userDao.findBy(UserDao.COLUMN_USERNAME, request.getUsername()))
          .thenReturn(user(USER_ID + 1, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userUcc.edit(request, USER_ID, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.USERNAME_TAKEN);
      verify(userDao, never()).update(any());
    }

    @Test
    void theEmailOfAnotherAccountIsTaken() {
      UserDto request = request();
      when(userDao.findBy(UserDao.COLUMN_USERNAME, request.getUsername())).thenReturn(null);
      when(userDao.findBy(UserDao.COLUMN_EMAIL, request.getEmail()))
          .thenReturn(user(USER_ID + 1, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userUcc.edit(request, USER_ID, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.EMAIL_TAKEN);
      verify(userDao, never()).update(any());
    }

    @Test
    void anUnknownAccountIsNotFound() {
      givenOptionExists("BIN");

      assertThatBusinessException(() -> userUcc.edit(request(), PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(userDao, never()).update(any());
    }
  }
}
