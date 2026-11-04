package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestUserService {

  @Autowired private ApplicationContext context;

  private UserRepository userRepository;
  private UserService userService;
  private MockDtoFactory mockDtoFactory;
  private User prof;
  private User stud;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    userRepository = context.getBean(UserRepository.class);
    userService = context.getBean(UserService.class);
    mockDtoFactory = new MockDtoFactory();
    prof = mockDtoFactory.getUser(Role.PROFESSOR);
    stud = mockDtoFactory.getUser(Role.STUDENT);
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
    assertEquals(prof.getId(), (userRepository.findById(prof.getId()).orElse(null)).getId());
  }

  @Test
  public void testCreateTC3() {
    userService.signup(stud);
    userService.signup(stud);
    assertEquals(2, (userRepository.findAll().size()));
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
    assertEquals(Role.PROFESSOR, stud.getRole());
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

  /** Signs up a professor (the first user) then a student, and returns the student. */
  private User signUpProfessorThenStudent() {
    userService.signup(prof);
    stud.setUsername("student1");
    stud.setEmail("student1@student.test");
    return userService.signup(stud);
  }

  @Test
  public void promoteToProfessorByIdIncrementsTheVersion() {
    User student = signUpProfessorThenStudent();
    int version = student.getVersion();

    userService.promoteToProfessor(student.getId());

    User promoted = userRepository.findById(student.getId()).orElse(null);
    assertEquals(Role.PROFESSOR, promoted.getRole());
    assertEquals(version + 1, promoted.getVersion());
  }

  @Test
  public void promoteToProfessorByIdLeavesAProfessorUntouched() {
    signUpProfessorThenStudent();
    int version = prof.getVersion();

    userService.promoteToProfessor(prof.getId());

    assertEquals(Role.PROFESSOR, userRepository.findById(prof.getId()).orElseThrow().getRole());
    assertEquals(version, userRepository.findById(prof.getId()).orElseThrow().getVersion());
  }

  @Test
  public void promoteToProfessorByUsernamePromotesThatStudent() {
    User student = signUpProfessorThenStudent();
    int version = student.getVersion();

    User promoted = userService.promoteToProfessorByUsername("student1");

    assertEquals(student.getId(), promoted.getId());
    assertEquals(Role.PROFESSOR, promoted.getRole());
    assertEquals(version + 1, promoted.getVersion());
    assertEquals(Role.PROFESSOR, userRepository.findById(student.getId()).orElseThrow().getRole());
  }

  @Test
  public void promoteToProfessorByUsernameLeavesAProfessorUntouched() {
    signUpProfessorThenStudent();
    int version = prof.getVersion();

    User same = userService.promoteToProfessorByUsername(prof.getUsername());

    assertEquals(Role.PROFESSOR, same.getRole());
    assertEquals(version, same.getVersion());
  }

  @Test
  public void promoteToProfessorByUsernameOfAnUnknownUserIsNotFound() {
    signUpProfessorThenStudent();

    assertThrows(
        ResourceNotFoundException.class, () -> userService.promoteToProfessorByUsername("nobody"));
  }

  // Edit d'un User avec des données déjà existantes dans un autre User
  @Test
  public void testEditTC4() {
    assertThrows(
        BusinessException.class,
        () -> {
          userService.signup(prof);
          userService.edit(stud, 1, Role.PROFESSOR);
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
          userService.edit(stud, 1, Role.PROFESSOR);
        });
  }
}
