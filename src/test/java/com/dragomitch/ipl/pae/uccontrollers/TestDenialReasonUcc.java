package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockDenialReasonDao;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestDenialReasonUcc {

  @Autowired
  private ApplicationContext context;
  private EntityFactory entityFactory;
  private DenialReasonDao denialReasonDao;
  private DenialReasonUcc denialReasonUcc;
  private MockDtoFactory mockDtoFactory;
  private DenialReasonDto denialReason;


  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    denialReasonDao = context.getBean(DenialReasonDao.class);
    denialReasonUcc = context.getBean(DenialReasonUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    denialReason = mockDtoFactory.getDenialReason();
  }

  /**
   * Cleans up the 'database'.
   */
  @AfterEach
  public void cleanUp() {
    ((MockDenialReasonDao) denialReasonDao).empty();
  }

  @Test
  public void testShowAllTC1() {
    ((MockDenialReasonDao) denialReasonDao).empty();
    assertEquals(0, denialReasonUcc.showAll().size(), "The database should be empty");
  }

  @Test
  public void testShowAllTC2() {
    denialReasonUcc.create(denialReason);
    assertEquals(1, denialReasonUcc.showAll().size(), "The database should countain 1 denial reason");
  }

  @Test
  public void testCreateTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      denialReasonUcc.create(null);
    });
  }

  @Test
  public void testCreateTC2() {
    denialReasonUcc.create(denialReason);
    assertEquals(1, denialReasonUcc.showAll().size(), "The database should contain only 1 denialreason");
  }

  @Test
  public void testCreateTC3() {
    denialReasonUcc.create(denialReason);
    assertEquals(1, denialReason.getId());
  }


  @Test
  public void testCreateTC4() {
    assertEquals(List.of("create.denialReason.reason:Size"),
        Violations.thrownBy(() -> {
          denialReason.setReason("Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
              + "Fusce quam orci, pharetra finibus porttitor vel, consequat vel arcu. "
              + "Cras tempus consequat lectus id imperdiet. "
              + "Sed tincidunt finibus odio eu condimentum. " + "Pellentesque aliquam placerat risus. "
              + "Phasellus lorem massa, placerat ut quam id volutpat.");
          denialReasonUcc.create(denialReason);
        }));
  }

  @Test
  public void aReasonLongerThanTheColumnIsReportedOnceAsTooLong() {
    denialReason.setReason("x".repeat(DenialReasonDto.REASON_MAX_LENGTH + 1));

    assertEquals(List.of("create.denialReason.reason:Size"),
        Violations.thrownBy(() -> denialReasonUcc.create(denialReason)));
    assertEquals(0, denialReasonUcc.showAll().size());
  }

  @Test
  public void testEditTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      denialReasonUcc.edit(0, denialReason);
    });
  }

  @Test
  public void testEditTC2() {
    assertThrows(ConstraintViolationException.class, () -> {
      denialReasonUcc.edit(1, null);
    });
  }

  @Test
  public void testEditTC3() {
    assertThrows(ResourceNotFoundException.class, () -> {
      denialReasonUcc.edit(1, denialReason);
    });
  }

  @Test
  public void testEditTC4() {
    denialReasonUcc.create(denialReason);
    String newOne = "Autre soEver";
    denialReason.setReason(newOne);
    denialReasonUcc.edit(1, denialReason);
    assertEquals(newOne, denialReasonUcc.showAll().get(0).getReason(), "The update is not correctly done for the field reason");
  }

  @Test
  public void editUpdatesTheReasonIdentifiedByThePath() {
    denialReasonUcc.create(denialReason);
    DenialReasonDto second = mockDtoFactory.getDenialReason();
    second.setReason("Second");
    denialReasonUcc.create(second);
    DenialReasonDto body = mockDtoFactory.getDenialReason();
    body.setId(1);
    body.setReason("Edited");

    DenialReasonDto edited = denialReasonUcc.edit(2, body);

    assertEquals(2, edited.getId());
    assertEquals("no reason whatsoever", denialReasonDao.findById(1).getReason());
    assertEquals("Edited", denialReasonDao.findById(2).getReason());
  }

  @Test
  public void testEditTC5() {
    denialReasonUcc.create(denialReason);
    DenialReasonDto reason = denialReasonUcc.edit(1, denialReason);
    assertEquals(1, reason.getVersion(), "The update is not correctly done for the version");
  }


}
