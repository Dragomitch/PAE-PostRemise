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
}
