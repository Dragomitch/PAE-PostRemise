package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestUserService {

  @Autowired private ApplicationContext context;

  private EntityFactory entityFactory;
  private UserRepository userRepository;
  private UserService userService;
  private MockDtoFactory mockDtoFactory;
  private UserDto prof;
  private UserDto stud;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    userRepository = context.getBean(UserRepository.class);
    userService = context.getBean(UserService.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    prof = mockDtoFactory.getUser(UserDto.ROLE_PROFESSOR);
    stud = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryUserRepository) userRepository).empty();
  }

  @Test
  public void testCreateTC1() {
    userService.signup(prof);
    prof.setId(1);
    assertEquals(prof.getId(), (userRepository.findById(prof.getId())).getId());
  }

  @Test
  public void testCreateTC2() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          userService.signup(null);
          prof.setId(1);
          assertEquals(prof.getId(), (userRepository.findById(prof.getId())).getId());
        });
  }

  @Test
  public void testCreateTC3() {
    userService.signup(stud);
    userService.signup(stud);
    assertEquals(2, (userRepository.findAll().size()));
  }

  @Test
  public void testCreateTC4() {
    assertEquals(
        List.of("signup.user.option.code:Size"),
        Violations.thrownBy(() -> {
          OptionDto option = mockDtoFactory.getOption();
          option.setCode("Wrong Code Man");
          stud.setOption(option);
          userService.signup(stud);
        }));
  }

  @Test
  public void testCreateTC5() {
    assertThrows(
        BusinessException.class,
        () -> {
          userService.signup(stud);
          userService.signup(prof);
        });
  }

  @Test
  public void testCreateTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          userService.signup(stud);
          prof.setUsername("chikiBriki");
          userService.signup(prof);
        });
  }

  @Test
  public void testCreateTC7() {
    assertThrows(
        BusinessException.class,
        () -> {
          userService.signup(stud);
          prof.setEmail("MamyFaitDesBlagues@joke.be");
          userService.signup(prof);
        });
  }

  @Test
  public void testShowAllTC1() {
    userService.signup(stud);
    prof.setUsername("chikiBriki");
    prof.setEmail("MamyFaitDesBlagues@joke.be");
    userService.signup(prof);
    assertEquals(2, userRepository.findAll().size());
  }

  @Test
  public void testPromoteToProfessorTC1() {
    userService.signup(stud);
    userService.promoteToProfessor(stud.getId());
    assertEquals(UserDto.ROLE_PROFESSOR, stud.getRole());
  }

  @Test
  public void testPromoteToProfessorTC2() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          userService.signup(stud);
          userService.promoteToProfessor(69);
        });
  }

  @Test
  public void testPromoteToProfessorTC3() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          userService.signup(stud);
          userService.promoteToProfessor(-1);
        });
  }

  /** Signs up a professor (the first user) then a student, and returns the student. */
  private UserDto signUpProfessorThenStudent() {
    userService.signup(prof);
    stud.setUsername("student1");
    stud.setEmail("student1@student.test");
    return userService.signup(stud);
  }

  @Test
  public void promoteToProfessorByIdIncrementsTheVersion() {
    UserDto student = signUpProfessorThenStudent();
    int version = student.getVersion();

    userService.promoteToProfessor(student.getId());

    UserDto promoted = userRepository.findById(student.getId());
    assertEquals(UserDto.ROLE_PROFESSOR, promoted.getRole());
    assertEquals(version + 1, promoted.getVersion());
  }

  @Test
  public void promoteToProfessorByIdLeavesAProfessorUntouched() {
    signUpProfessorThenStudent();
    int version = prof.getVersion();

    userService.promoteToProfessor(prof.getId());

    assertEquals(UserDto.ROLE_PROFESSOR, userRepository.findById(prof.getId()).getRole());
    assertEquals(version, userRepository.findById(prof.getId()).getVersion());
  }

  @Test
  public void promoteToProfessorByUsernamePromotesThatStudent() {
    UserDto student = signUpProfessorThenStudent();
    int version = student.getVersion();

    UserDto promoted = userService.promoteToProfessorByUsername("student1");

    assertEquals(student.getId(), promoted.getId());
    assertEquals(UserDto.ROLE_PROFESSOR, promoted.getRole());
    assertEquals(version + 1, promoted.getVersion());
    assertEquals(UserDto.ROLE_PROFESSOR, userRepository.findById(student.getId()).getRole());
  }

  @Test
  public void promoteToProfessorByUsernameLeavesAProfessorUntouched() {
    signUpProfessorThenStudent();
    int version = prof.getVersion();

    UserDto same = userService.promoteToProfessorByUsername(prof.getUsername());

    assertEquals(UserDto.ROLE_PROFESSOR, same.getRole());
    assertEquals(version, same.getVersion());
  }

  @Test
  public void promoteToProfessorByUsernameOfAnUnknownUserIsNotFound() {
    signUpProfessorThenStudent();

    assertThrows(
        ResourceNotFoundException.class, () -> userService.promoteToProfessorByUsername("nobody"));
  }

  @Test
  public void promoteToProfessorByUsernameNeedsAUsername() {
    assertEquals(
        List.of("promoteToProfessorByUsername.username:NotBlank"),
        Violations.thrownBy(() -> userService.promoteToProfessorByUsername("")));
    assertEquals(
        List.of("promoteToProfessorByUsername.username:NotBlank"),
        Violations.thrownBy(() -> userService.promoteToProfessorByUsername(null)));
    assertEquals(
        List.of("promoteToProfessorByUsername.username:Size"),
        Violations.thrownBy(() -> userService.promoteToProfessorByUsername("x".repeat(21))));
  }

  @Test
  public void testEditTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          userService.signup(stud);
          userService.edit(null, 1, UserDto.ROLE_STUDENT);
        });
  }

  @Test
  public void testEditTC2() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          userService.signup(stud);
          userService.edit(stud, -1, UserDto.ROLE_STUDENT);
        });
  }

  @Test
  public void testEditTC3() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          userService.signup(stud);
          userService.edit(stud, -1, "");
        });
  }

  // Edit d'un User avec des données déjà existantes dans un autre User
  @Test
  public void testEditTC4() {
    assertThrows(
        BusinessException.class,
        () -> {
          userService.signup(prof);
          userService.edit(stud, 1, UserDto.ROLE_PROFESSOR);
        });
  }

  @Test
  public void testEditTC5() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          userService.signup(prof);
          prof.setUsername("chikiBriki");
          prof.setEmail("MamyFaitDesBlagues@joke.be");
          userService.edit(stud, 1, UserDto.ROLE_PROFESSOR);
        });
  }
}
