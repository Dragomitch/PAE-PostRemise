package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryNominatedStudentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.NominatedStudentService;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestNominatedStudentService {

  @Autowired private ApplicationContext context;

  private EntityFactory entityFactory;
  private NominatedStudentRepository nominatedStudentRepository;
  private NominatedStudentService nominatedStudentService;
  private MockDtoFactory mockDtoFactory;
  private NominatedStudentDto nominatedStud;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    nominatedStudentRepository = context.getBean(NominatedStudentRepository.class);
    nominatedStudentService = context.getBean(NominatedStudentService.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    nominatedStud = mockDtoFactory.getNominatedStudent();
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryNominatedStudentRepository) nominatedStudentRepository).empty();
  }

  @Test
  public void testCreateTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          nominatedStudentService.create(null, 1, Role.PROFESSOR);
        });
  }

  @Test
  public void testCreateTC2() {
    assertEquals(
        List.of(
            "create.nominatedStudent.bic:Bic",
            "create.nominatedStudent.birthdate:Past",
            "create.nominatedStudent.iban:Iban"),
        Violations.thrownBy(() -> {
          nominatedStudentRepository.create(nominatedStud);
          nominatedStudentService.create(nominatedStud, 1, Role.PROFESSOR);
          assertEquals(1, nominatedStudentRepository.findAll().size());
        }));
  }

  @Test
  public void createKeepsTheVersionOfTheUserAndReturnsIt() {
    InMemoryUserRepository userRepository = context.getBean(InMemoryUserRepository.class);
    userRepository.empty();
    UserDto user = userRepository.create(mockDtoFactory.getUser(UserDto.ROLE_STUDENT));
    user.setVersion(3);
    NominatedStudentDto student = validStudent(user.getId());

    NominatedStudentDto created =
        nominatedStudentService.create(student, user.getId(), Role.STUDENT);

    assertEquals(3, created.getVersion(), "the returned version is the stored one");
    assertEquals(3, nominatedStudentRepository.findById(user.getId()).getVersion());
    userRepository.empty();
  }

  private NominatedStudentDto validStudent(int userId) {
    NominatedStudentDto student = mockDtoFactory.getNominatedStudent();
    student.setId(userId);
    student.setIban("BE68539007547034");
    student.setBic("GKCCBEBB");
    student.setBirthdate(java.time.LocalDate.of(2000, 1, 1));
    return student;
  }
}
