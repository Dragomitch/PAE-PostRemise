package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockDenialReasonDao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;

public class TestDenialReasonUcc {
  private EntityFactory entityFactory;
  private DenialReasonDao denialReasonDao;
  private DenialReasonUcc denialReasonUcc;
  private MockDtoFactory mockDtoFactory;
  private DenialReasonDto denialReason;


  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    denialReasonDao = DependencyManager.getInstance(DenialReasonDao.class);
    denialReasonUcc = DependencyManager.getInstance(DenialReasonUcc.class);
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
    assertThrows(IllegalArgumentException.class, () -> {
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
    assertThrows(BusinessException.class, () -> {
      denialReason.setReason("Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
          + "Fusce quam orci, pharetra finibus porttitor vel, consequat vel arcu. "
          + "Cras tempus consequat lectus id imperdiet. "
          + "Sed tincidunt finibus odio eu condimentum. " + "Pellentesque aliquam placerat risus. "
          + "Phasellus lorem massa, placerat ut quam id volutpat.");
      denialReasonUcc.create(denialReason);
    });
  }

  @Test
  public void testEditTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      denialReasonUcc.edit(0, denialReason);
    });
  }

  @Test
  public void testEditTC2() {
    assertThrows(IllegalArgumentException.class, () -> {
      denialReasonUcc.edit(1, null);
    });
  }

  @Test
  public void testEditTC3() {
    assertThrows(RessourceNotFoundException.class, () -> {
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
  public void testEditTC5() {
    denialReasonUcc.create(denialReason);
    DenialReasonDto reason = denialReasonUcc.edit(1, denialReason);
    assertEquals(1, reason.getVersion(), "The update is not correctly done for the version");
  }


}
