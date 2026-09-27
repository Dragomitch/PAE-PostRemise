package com.dragomitch.ipl.pae.uccontrollers;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.context.ErrorManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockAddressDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerOptionDao;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

public class TestPartnerUcc {

  private EntityFactory entityFactory;
  private PartnerDao partnerDao;
  private PartnerUcc partnerUcc;
  private MockDtoFactory mockDtoFactory;
  private PartnerDto partnerDto;
  private AddressDao addressDao;
  private PartnerOptionDao partnerOptionDao;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    ErrorManager.load();
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    addressDao = DependencyManager.getInstance(AddressDao.class);
    partnerDao = DependencyManager.getInstance(PartnerDao.class);
    partnerOptionDao = DependencyManager.getInstance(PartnerOptionDao.class);
    partnerUcc = DependencyManager.getInstance(PartnerUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    partnerDto = mockDtoFactory.getPartner();
  }

  /**
   * Cleans up the 'database'.
   */
  @AfterEach
  public void cleanUp() {
    ((MockPartnerDao) partnerDao).empty();
    ((MockAddressDao) addressDao).empty();
    ((MockPartnerOptionDao) partnerOptionDao).empty();
  }

  @Test
  public void testAddOptionTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      PartnerOptionDto option = mockDtoFactory.getPartnerOption();
      partnerUcc.addOption(0, option); // L'id doit être > 0
    });
  }

  @Test
  public void testAddOptionTC2() {
    assertThrows(IllegalArgumentException.class, () -> {
      partnerUcc.addOption(1, null); // Le PartnerOption doit être différent de null
    });
  }

  @Test
  public void testAddOptionTC3() {
    assertThrows(IllegalArgumentException.class, () -> {
      PartnerOptionDto option = mockDtoFactory.getPartnerOption();
      option.setCode("");
      partnerUcc.addOption(1, option); // le code doit être une String valide
    });
  }

  @Test
  public void testAddOptionTC4() {
    assertThrows(IllegalArgumentException.class, () -> {
      PartnerOptionDto option = mockDtoFactory.getPartnerOption();
      option.setDepartement("");
      partnerUcc.addOption(1, option); // le departement doit être une String valide
    });
  }

  @Test
  public void testAddOptionTC5() {
    assertThrows(RessourceNotFoundException.class, () -> {
      PartnerDto partner = partnerDao.create(partnerDto);
      PartnerOptionDto option = partner.getOptions().get(0);
      option.setCode("Bouilla");
      partnerUcc.addOption(1, option); // Il doit exister une option avec le bon OptionCode
    });
  }

  @Test
  public void testCreateTC1() {
    assertThrows(InsufficientPermissionException.class, () -> {
      partnerDto.setStatus(true);
      partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    });
  }

  @Test
  public void testCreateTC2() {
    String filter = "all";
    String option = "BIN";
    String value = "";
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals(0, partnerDao.findAll(filter, value, User.ROLE_STUDENT, option).size(), "There should be only 1 partner");
  }

  @Test
  public void testCreateTC3() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals(addressDao.findById(partnerDto.getAddress().getId()), partnerDto.getAddress(), "The address inserted is not the same one");
  }

  @Test
  public void testCreateTC4() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals(partnerDao.findById(partnerDto.getId()), partnerDto, "The partner inserted is the one given");
  }

  @Test
  public void testCreateTC5() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals(partnerDao.findById(partnerDto.getId()), partnerDto, "The partner inserted is the one given");
  }

  @Test
  public void testShowAllTC1() {
    String filter = "all";
    String option = "BIN";
    String value = "";
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_PROFESSOR);
    assertEquals(1, partnerDao.findAll(filter, value, User.ROLE_PROFESSOR, option).size(), "There should be only 1 partner");
  }

  @Test
  public void testShowAllTC2() {
    String filter = "all";
    String option = "BIM";
    String value = "";
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals(0, partnerDao.findAll(filter, value, User.ROLE_STUDENT, option).size());
  }

  @Test
  public void testShowAllTC3() {
    String filter = "all";
    String option = "BIN";
    String value = "";
    partnerDto.setStatus(true);
    partnerUcc.create(partnerDto, User.ROLE_PROFESSOR);
    assertEquals(1, partnerDao.findAll(filter, value, User.ROLE_STUDENT, option).size());
  }

  @Test
  public void testShowAllTC4() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    PartnerDto partner = mockDtoFactory.getPartner();
    partner.setStatus(true);
    partnerUcc.create(partner, User.ROLE_PROFESSOR);
    String filter = "all";
    String option = "BIN";
    String value = "";
    assertEquals(2, partnerDao.findAll(filter, value, User.ROLE_PROFESSOR, option).size());
  }

  @Test
  public void testShowAllTC5() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    PartnerDto partner = mockDtoFactory.getPartner();
    partner.setStatus(true);
    partnerUcc.create(partner, User.ROLE_PROFESSOR);
    String filter = "all";
    String option = "BIN";
    String value = "";
    assertEquals(1, partnerDao.findAll(filter, value, User.ROLE_STUDENT, option).size());
  }

}
