package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockNominatedStudentDao;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestNominatedStudentUcc {

  @Autowired
  private ApplicationContext context;

  private EntityFactory entityFactory;
  private NominatedStudentDao nominatedStudentDao;
  private NominatedStudentUcc nominatedStudentUcc;
  private MockDtoFactory mockDtoFactory;
  private NominatedStudentDto nominatedStud;

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    nominatedStudentDao = context.getBean(NominatedStudentDao.class);
    nominatedStudentUcc = context.getBean(NominatedStudentUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    nominatedStud = mockDtoFactory.getNominatedStudent();
  }

  /**
   * Cleans up the 'database'.
   */
  @AfterEach
  public void cleanUp() {
    ((MockNominatedStudentDao) nominatedStudentDao).empty();
  }

  @Test
  public void testCreateTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      nominatedStudentUcc.create(null, 1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCreateTC2() {
    assertEquals(List.of("create.nominatedStudent.bic:Bic", "create.nominatedStudent.birthdate:Past", "create.nominatedStudent.iban:Iban"),
        Violations.thrownBy(() -> {
          nominatedStudentDao.create(nominatedStud);
          nominatedStudentUcc.create(nominatedStud, 1, UserDto.ROLE_PROFESSOR);
          assertEquals(1, nominatedStudentDao.findAll().size());
        }));
  }

  @Test
  public void createKeepsTheVersionOfTheUserAndReturnsIt() {
    MockUserDao userDao = context.getBean(MockUserDao.class);
    userDao.empty();
    UserDto user = userDao.create(mockDtoFactory.getUser(UserDto.ROLE_STUDENT));
    user.setVersion(3);
    NominatedStudentDto student = validStudent(user.getId());

    NominatedStudentDto created =
        nominatedStudentUcc.create(student, user.getId(), UserDto.ROLE_STUDENT);

    assertEquals(3, created.getVersion(), "the returned version is the stored one");
    assertEquals(3, nominatedStudentDao.findById(user.getId()).getVersion());
    userDao.empty();
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
