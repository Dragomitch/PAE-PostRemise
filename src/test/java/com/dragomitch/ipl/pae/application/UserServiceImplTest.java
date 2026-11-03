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

import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.EntityFactories;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.application.MockDtoFactory;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ConcurrentModificationException;
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
class UserServiceImplTest {

  /** The instant the use case reads from its clock: the dates it sets are asserted exactly. */
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 15, 10, 15, 30);
  private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private static final int USER_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final LocalDateTime REGISTERED = LocalDateTime.of(2015, 9, 14, 8, 0);

  @Mock
  private UserRepository userRepository;
  @Mock
  private OptionRepository optionRepository;
  @Mock
  private NominatedStudentRepository nominatedStudentRepository;
  @Mock
  private PasswordEncoder passwordEncoder;

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, optionRepository, nominatedStudentRepository, passwordEncoder,
        CLOCK);
  }

  private UserDto user(int id, String role) {
    UserDto user = dtos.getUser(role);
    user.setId(id);
    return user;
  }

  private void givenOptionExists(String code) {
    when(optionRepository.findByCode(code)).thenReturn((OptionDto) entityFactory.build(OptionDto.class));
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
      when(userRepository.isEmpty()).thenReturn(noUserYet);
      when(passwordEncoder.encode("plain password")).thenReturn("$2a$hash");
      when(userRepository.create(user)).thenReturn(user);

      UserDto created = userService.signup(user);

      assertThat(created.getRole())
          .isEqualTo(noUserYet ? UserDto.ROLE_PROFESSOR : UserDto.ROLE_STUDENT);
      assertThat(created.getPassword()).isEqualTo("$2a$hash");
      assertThat(created.getRegistrationDate()).isEqualTo(NOW);
    }

    @Test
    void theUsernameMustBeFree() {
      UserDto user = newUser();
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, user.getUsername()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userService.signup(user))
          .hasErrorCode(ErrorCode.USERNAME_TAKEN).hasArguments(user.getUsername());
      verify(userRepository, never()).create(any());
    }

    @Test
    void theEmailMustBeFree() {
      UserDto user = newUser();
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, user.getUsername())).thenReturn(null);
      when(userRepository.findBy(UserRepository.COLUMN_EMAIL, user.getEmail()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userService.signup(user))
          .hasErrorCode(ErrorCode.EMAIL_TAKEN).hasArguments(user.getEmail());
      verify(userRepository, never()).create(any());
    }

    @Test
    void theOptionMustExist() {
      UserDto user = newUser();
      user.getOption().setCode("XYZ");

      assertThatBusinessException(() -> userService.signup(user))
          .hasErrorCode(ErrorCode.UNKNOWN_OPTION).hasArguments("XYZ");
      verify(userRepository, never()).create(any());
      verifyNoInteractions(passwordEncoder);
    }
  }

  @Test
  void showAllListsEveryUser() {
    List<UserDto> all = List.of(user(USER_ID, UserDto.ROLE_STUDENT));
    when(userRepository.findAll()).thenReturn(all);

    assertThat(userService.showAll()).isSameAs(all);
  }

  @Nested
  class PromoteToProfessor {

    @Test
    void aStudentBecomesAProfessorWithTheVersionOfTheRepository() {
      UserDto student = user(USER_ID, UserDto.ROLE_STUDENT);
      student.setVersion(4);
      when(userRepository.findById(USER_ID)).thenReturn(student);
      when(userRepository.promoteToProfessor(USER_ID, 4)).thenReturn(5);

      userService.promoteToProfessor(USER_ID);

      assertThat(student.getRole()).isEqualTo(UserDto.ROLE_PROFESSOR);
      assertThat(student.getVersion()).isEqualTo(5);
      verify(userRepository).promoteToProfessor(USER_ID, 4);
      verify(userRepository, never()).update(any());
    }

    @Test
    void promotingAProfessorWritesNothing() {
      when(userRepository.findById(PROFESSOR_ID)).thenReturn(user(PROFESSOR_ID, UserDto.ROLE_PROFESSOR));

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
      UserDto student = user(USER_ID, UserDto.ROLE_STUDENT);
      when(userRepository.findById(USER_ID)).thenReturn(student);
      when(userRepository.promoteToProfessor(USER_ID, student.getVersion()))
          .thenThrow(new ConcurrentModificationException());

      assertThatThrownBy(() -> userService.promoteToProfessor(USER_ID))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(student.getRole()).isEqualTo(UserDto.ROLE_STUDENT);
    }
  }

  @Nested
  class PromoteToProfessorByUsername {

    private static final String USERNAME = "alice";

    @Test
    void aStudentBecomesAProfessorAndIsReturned() {
      UserDto student = user(USER_ID, UserDto.ROLE_STUDENT);
      student.setVersion(2);
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, USERNAME)).thenReturn(student);
      when(userRepository.promoteToProfessor(USERNAME, 2)).thenReturn(3);

      UserDto promoted = userService.promoteToProfessorByUsername(USERNAME);

      assertThat(promoted).isSameAs(student);
      assertThat(promoted.getRole()).isEqualTo(UserDto.ROLE_PROFESSOR);
      assertThat(promoted.getVersion()).isEqualTo(3);
    }

    @Test
    void aProfessorIsReturnedUnchanged() {
      UserDto professor = user(PROFESSOR_ID, UserDto.ROLE_PROFESSOR);
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, USERNAME)).thenReturn(professor);

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
      UserDto student = user(USER_ID, UserDto.ROLE_STUDENT);
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, USERNAME)).thenReturn(student);
      when(userRepository.promoteToProfessor(USERNAME, student.getVersion()))
          .thenThrow(new ConcurrentModificationException());

      assertThatThrownBy(() -> userService.promoteToProfessorByUsername(USERNAME))
          .isInstanceOf(ConcurrentModificationException.class);
    }
  }

  @Nested
  class Edit {

    /** The account as stored: its password hash, registration date and role. */
    private UserDto givenStoredUser(String role) {
      UserDto stored = user(USER_ID, role);
      stored.setPassword("$2a$stored-hash");
      stored.setRegistrationDate(REGISTERED);
      when(userRepository.findById(USER_ID)).thenReturn(stored);
      return stored;
    }

    /** {@link UserRepository#update} copies the new version of the row into the DTO. */
    private void givenUpdateIncrementsTheVersion() {
      doAnswer(invocation -> {
        UserDto user = invocation.getArgument(0);
        user.setVersion(user.getVersion() + 1);
        return null;
      }).when(userRepository).update(any());
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

      UserDto edited = userService.edit(request, requester, role);

      assertThat(edited.getLastName()).isEqualTo("Martin-Dupont");
      assertThat(edited.getPassword()).isEqualTo("$2a$stored-hash");
      assertThat(edited.getRegistrationDate()).isEqualTo(REGISTERED);
      assertThat(edited.getRole()).isEqualTo(UserDto.ROLE_STUDENT);
      verify(userRepository).update(request);
    }

    @Test
    void theOwnUsernameAndEmailMayBeKept() {
      UserDto request = request();
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, request.getUsername()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));
      when(userRepository.findBy(UserRepository.COLUMN_EMAIL, request.getEmail()))
          .thenReturn(user(USER_ID, UserDto.ROLE_STUDENT));
      givenOptionExists("BIN");
      givenStoredUser(UserDto.ROLE_STUDENT);

      assertThat(userService.edit(request, USER_ID, UserDto.ROLE_STUDENT)).isSameAs(request);
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
      when(nominatedStudentRepository.findById(USER_ID)).thenReturn(student);

      userService.edit(request, USER_ID, UserDto.ROLE_STUDENT);

      verify(nominatedStudentRepository).update(student);
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
      when(nominatedStudentRepository.findById(USER_ID)).thenReturn(student);

      userService.edit(request, USER_ID, UserDto.ROLE_STUDENT);

      verify(nominatedStudentRepository, never()).update(any());
    }

    @Test
    void aStudentCannotEditAnotherAccount() {
      assertThatBusinessException(() -> userService.edit(request(), USER_ID + 1,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(userRepository, optionRepository, nominatedStudentRepository);
    }

    @Test
    void theUsernameOfAnotherAccountIsTaken() {
      UserDto request = request();
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, request.getUsername()))
          .thenReturn(user(USER_ID + 1, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userService.edit(request, USER_ID, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.USERNAME_TAKEN);
      verify(userRepository, never()).update(any());
    }

    @Test
    void theEmailOfAnotherAccountIsTaken() {
      UserDto request = request();
      when(userRepository.findBy(UserRepository.COLUMN_USERNAME, request.getUsername())).thenReturn(null);
      when(userRepository.findBy(UserRepository.COLUMN_EMAIL, request.getEmail()))
          .thenReturn(user(USER_ID + 1, UserDto.ROLE_STUDENT));

      assertThatBusinessException(() -> userService.edit(request, USER_ID, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.EMAIL_TAKEN);
      verify(userRepository, never()).update(any());
    }

    @Test
    void anUnknownAccountIsNotFound() {
      givenOptionExists("BIN");

      assertThatBusinessException(() -> userService.edit(request(), PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(userRepository, never()).update(any());
    }
  }
}
