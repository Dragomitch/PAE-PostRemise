package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PasswordHasher;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * User accounts: registration (first user is a professor), promotion, edition by the owner or a
 * professor, unique username and email, existing option.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  /** The instant the use case reads from its clock: the dates it sets are asserted exactly. */
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 15, 10, 15, 30);

  private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private static final int USER_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final LocalDateTime REGISTERED = LocalDateTime.of(2015, 9, 14, 8, 0);

  @Mock private UserRepository userRepository;
  @Mock private OptionRepository optionRepository;
  @Mock private NominatedStudentRepository nominatedStudentRepository;
  @Mock private PasswordHasher passwordHasher;

  private final MockDtoFactory dtos = new MockDtoFactory();
  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserServiceImpl(
            userRepository, optionRepository, nominatedStudentRepository, passwordHasher, CLOCK);
  }

  private User user(int id, Role role) {
    User user = dtos.getUser(role);
    user.setId(id);
    return user;
  }

  private void givenOptionExists(String code) {
    when(optionRepository.findByCode(code)).thenReturn(Optional.of(new Option()));
  }

  @Nested
  class Signup {

    private User newUser() {
      User user = user(0, null);
      user.setPassword("plain password");
      user.setRegistrationDate(null);
      return user;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void theFirstUserIsAProfessorTheOthersAreStudents(boolean noUserYet) {
      User user = newUser();
      givenOptionExists("BIN");
      when(userRepository.isEmpty()).thenReturn(noUserYet);
      when(passwordHasher.hash("plain password")).thenReturn("$2a$hash");
      when(userRepository.create(user)).thenReturn(user);

      User created = userService.signup(user);

      assertThat(created.getRole()).isEqualTo(noUserYet ? Role.PROFESSOR : Role.STUDENT);
      assertThat(created.getPassword()).isEqualTo("$2a$hash");
      assertThat(created.getRegistrationDate()).isEqualTo(NOW);
    }

    @Test
    void theUsernameMustBeFree() {
      User user = newUser();
      when(userRepository.findByUsername(user.getUsername()))
          .thenReturn(Optional.of(user(USER_ID, Role.STUDENT)));

      assertThatBusinessException(() -> userService.signup(user))
          .hasErrorCode(ErrorCode.USERNAME_TAKEN)
          .hasArguments(user.getUsername());
      verify(userRepository, never()).create(any());
    }

    @Test
    void theEmailMustBeFree() {
      User user = newUser();
      when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
      when(userRepository.findByEmail(user.getEmail()))
          .thenReturn(Optional.of(user(USER_ID, Role.STUDENT)));

      assertThatBusinessException(() -> userService.signup(user))
          .hasErrorCode(ErrorCode.EMAIL_TAKEN)
          .hasArguments(user.getEmail());
      verify(userRepository, never()).create(any());
    }

    @Test
    void theOptionMustExist() {
      User user = newUser();
      user.getOption().setCode("XYZ");

      assertThatBusinessException(() -> userService.signup(user))
          .hasErrorCode(ErrorCode.UNKNOWN_OPTION)
          .hasArguments("XYZ");
      verify(userRepository, never()).create(any());
      verifyNoInteractions(passwordHasher);
    }
  }

  @Test
  void showAllListsEveryUser() {
    List<User> all = List.of(user(USER_ID, Role.STUDENT));
    when(userRepository.findAll()).thenReturn(all);

    assertThat(userService.showAll()).isSameAs(all);
  }

  @Nested
  class PromoteToProfessor {

    @Test
    void aStudentBecomesAProfessorWithTheVersionOfTheRepository() {
      User student = user(USER_ID, Role.STUDENT);
      student.setVersion(4);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(student));
      when(userRepository.promoteToProfessor(USER_ID, 4)).thenReturn(5);

      userService.promoteToProfessor(USER_ID);

      assertThat(student.getRole()).isEqualTo(Role.PROFESSOR);
      assertThat(student.getVersion()).isEqualTo(5);
      verify(userRepository).promoteToProfessor(USER_ID, 4);
      verify(userRepository, never()).update(any());
    }

    @Test
    void promotingAProfessorWritesNothing() {
      when(userRepository.findById(PROFESSOR_ID))
          .thenReturn(Optional.of(user(PROFESSOR_ID, Role.PROFESSOR)));

      userService.promoteToProfessor(PROFESSOR_ID);

      verify(userRepository, never()).promoteToProfessor(anyInt(), anyInt());
      verify(userRepository, never()).update(any());
    }

    @Test
    void anUnknownUserIsNotFound() {
      assertThatBusinessException(() -> userService.promoteToProfessor(USER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(userRepository, never()).promoteToProfessor(anyInt(), anyInt());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      User student = user(USER_ID, Role.STUDENT);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(student));
      when(userRepository.promoteToProfessor(USER_ID, student.getVersion()))
          .thenThrow(new ConcurrentModificationException());

      assertThatThrownBy(() -> userService.promoteToProfessor(USER_ID))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(student.getRole()).isEqualTo(Role.STUDENT);
    }
  }

  @Nested
  class PromoteToProfessorByUsername {

    private static final String USERNAME = "alice";

    @Test
    void aStudentBecomesAProfessorAndIsReturned() {
      User student = user(USER_ID, Role.STUDENT);
      student.setVersion(2);
      when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(student));
      when(userRepository.promoteToProfessor(USERNAME, 2)).thenReturn(3);

      User promoted = userService.promoteToProfessorByUsername(USERNAME);

      assertThat(promoted).isSameAs(student);
      assertThat(promoted.getRole()).isEqualTo(Role.PROFESSOR);
      assertThat(promoted.getVersion()).isEqualTo(3);
    }

    @Test
    void aProfessorIsReturnedUnchanged() {
      User professor = user(PROFESSOR_ID, Role.PROFESSOR);
      when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(professor));

      assertThat(userService.promoteToProfessorByUsername(USERNAME)).isSameAs(professor);
      verify(userRepository, never()).promoteToProfessor(anyString(), anyInt());
    }

    @Test
    void anUnknownUsernameIsNotFound() {
      assertThatBusinessException(() -> userService.promoteToProfessorByUsername(USERNAME))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(userRepository, never()).promoteToProfessor(anyString(), anyInt());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      User student = user(USER_ID, Role.STUDENT);
      when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(student));
      when(userRepository.promoteToProfessor(USERNAME, student.getVersion()))
          .thenThrow(new ConcurrentModificationException());

      assertThatThrownBy(() -> userService.promoteToProfessorByUsername(USERNAME))
          .isInstanceOf(ConcurrentModificationException.class);
    }
  }

  @Nested
  class Edit {

    /** The account as stored: its password hash, registration date and role. */
    private User givenStoredUser(Role role) {
      User stored = user(USER_ID, role);
      stored.setPassword("$2a$stored-hash");
      stored.setRegistrationDate(REGISTERED);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stored));
      return stored;
    }

    /** {@link UserRepository#update} copies the new version of the row into the DTO. */
    private void givenUpdateIncrementsTheVersion() {
      doAnswer(
              invocation -> {
                User user = invocation.getArgument(0);
                user.setVersion(user.getVersion() + 1);
                return null;
              })
          .when(userRepository)
          .update(any());
    }

    /** The data sent by the client, trying to change what it may not. */
    private User request() {
      User request = user(USER_ID, Role.PROFESSOR);
      request.setLastName("Martin-Dupont");
      request.setPassword("new password");
      request.setRegistrationDate(LocalDateTime.now());
      request.setVersion(3);
      return request;
    }

    @ParameterizedTest
    @ValueSource(ints = {USER_ID, PROFESSOR_ID})
    void theOwnerOrAProfessorEditsTheAccountButNotItsPasswordDateOrRole(int requester) {
      Role role = requester == USER_ID ? Role.STUDENT : Role.PROFESSOR;
      User request = request();
      givenOptionExists("BIN");
      givenStoredUser(Role.STUDENT);

      User edited = userService.edit(request, requester, role);

      assertThat(edited.getLastName()).isEqualTo("Martin-Dupont");
      assertThat(edited.getPassword()).isEqualTo("$2a$stored-hash");
      assertThat(edited.getRegistrationDate()).isEqualTo(REGISTERED);
      assertThat(edited.getRole()).isEqualTo(Role.STUDENT);
      verify(userRepository).update(request);
    }

    @Test
    void theOwnUsernameAndEmailMayBeKept() {
      User request = request();
      when(userRepository.findByUsername(request.getUsername()))
          .thenReturn(Optional.of(user(USER_ID, Role.STUDENT)));
      when(userRepository.findByEmail(request.getEmail()))
          .thenReturn(Optional.of(user(USER_ID, Role.STUDENT)));
      givenOptionExists("BIN");
      givenStoredUser(Role.STUDENT);

      assertThat(userService.edit(request, USER_ID, Role.STUDENT)).isSameAs(request);
    }

    @Test
    void aStaleNominatedStudentRowIsBroughtToTheVersionOfTheUser() {
      User request = request();
      givenOptionExists("BIN");
      givenStoredUser(Role.STUDENT);
      givenUpdateIncrementsTheVersion();
      NominatedStudent student = new NominatedStudent();
      student.setId(USER_ID);
      student.setVersion(1);
      when(nominatedStudentRepository.findById(USER_ID)).thenReturn(Optional.of(student));

      userService.edit(request, USER_ID, Role.STUDENT);

      verify(nominatedStudentRepository).update(student);
    }

    @Test
    void aNominatedStudentRowAtTheVersionOfTheUserIsLeftAlone() {
      User request = request();
      givenOptionExists("BIN");
      givenStoredUser(Role.STUDENT);
      NominatedStudent student = new NominatedStudent();
      // already at the version the user row gets
      student.setVersion(request.getVersion() + 1);
      givenUpdateIncrementsTheVersion();
      when(nominatedStudentRepository.findById(USER_ID)).thenReturn(Optional.of(student));

      userService.edit(request, USER_ID, Role.STUDENT);

      verify(nominatedStudentRepository, never()).update(any());
    }

    @Test
    void aStudentCannotEditAnotherAccount() {
      assertThatBusinessException(() -> userService.edit(request(), USER_ID + 1, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(userRepository, optionRepository, nominatedStudentRepository);
    }

    @Test
    void theUsernameOfAnotherAccountIsTaken() {
      User request = request();
      when(userRepository.findByUsername(request.getUsername()))
          .thenReturn(Optional.of(user(USER_ID + 1, Role.STUDENT)));

      assertThatBusinessException(() -> userService.edit(request, USER_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.USERNAME_TAKEN);
      verify(userRepository, never()).update(any());
    }

    @Test
    void theEmailOfAnotherAccountIsTaken() {
      User request = request();
      when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
      when(userRepository.findByEmail(request.getEmail()))
          .thenReturn(Optional.of(user(USER_ID + 1, Role.STUDENT)));

      assertThatBusinessException(() -> userService.edit(request, USER_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.EMAIL_TAKEN);
      verify(userRepository, never()).update(any());
    }

    @Test
    void anUnknownAccountIsNotFound() {
      givenOptionExists("BIN");

      assertThatBusinessException(() -> userService.edit(request(), PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(userRepository, never()).update(any());
    }
  }
}
