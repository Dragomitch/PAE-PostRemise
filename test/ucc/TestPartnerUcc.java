package ucc;


import static org.junit.Assert.assertEquals;

import business.EntityFactory;
import business.User;
import business.dto.PartnerDto;
import business.dto.PartnerOptionDto;
import business.exceptions.RessourceNotFoundException;
import main.ContextManager;
import main.DependencyManager;
import main.ErrorManager;
import persistence.AddressDao;
import persistence.OptionDao;
import persistence.PartnerDao;
import persistence.PartnerOptionDao;
import persistence.mocks.MockAddressDao;
import persistence.mocks.MockPartnerDao;
import persistence.mocks.MockPartnerOptionDao;
import presentation.exceptions.InsufficientPermissionException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPartnerUcc {
  private EntityFactory entityFactory;
  private PartnerDao partnerDao;
  private PartnerUcc partnerUcc;
  private MockDtoFactory mockDtoFactory;
  private PartnerDto partnerDto;
  private AddressDao addressDao;
  private PartnerOptionDao partnerOptionDao;
  private OptionDao optionDao;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
  public void setUp() {
    ErrorManager.load();
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    addressDao = DependencyManager.getInstance(AddressDao.class);
    partnerDao = DependencyManager.getInstance(PartnerDao.class);
    partnerOptionDao = DependencyManager.getInstance(PartnerOptionDao.class);
    optionDao = DependencyManager.getInstance(OptionDao.class);
    partnerUcc = DependencyManager.getInstance(PartnerUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    partnerDto = mockDtoFactory.getPartner();
  }

  /**
   * Cleans up the 'database'.
   */
  @After
  public void cleanUp() {
    ((MockPartnerDao) partnerDao).empty();
    ((MockAddressDao) addressDao).empty();
    ((MockPartnerOptionDao) partnerOptionDao).empty();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddOptionTC1() {
    PartnerOptionDto option = mockDtoFactory.getPartnerOption();
    partnerUcc.addOption(0, option); // L'id doit être > 0
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddOptionTC2() {
    partnerUcc.addOption(1, null); // Le PartnerOption doit être différent de null
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddOptionTC3() {
    PartnerOptionDto option = mockDtoFactory.getPartnerOption();
    option.setCode("");
    partnerUcc.addOption(1, option); // le code doit être une String valide
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddOptionTC4() {
    PartnerOptionDto option = mockDtoFactory.getPartnerOption();
    option.setDepartement("");
    partnerUcc.addOption(1, option); // le departement doit être une String valide
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testAddOptionTC5() {
    PartnerDto partner = partnerDao.create(partnerDto);
    PartnerOptionDto option = partner.getOptions().get(0);
    option.setCode("Bouilla");
    partnerUcc.addOption(1, option); // Il doit exister une option avec le bon OptionCode
  }

  @Test(expected = InsufficientPermissionException.class)
  public void testCreateTC1() {
    partnerDto.setStatus(true);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
  }

  @Test
  public void testCreateTC2() {
    String filter = "all";
    String option = "BIN";
    String value = "";
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals("There should be only 1 partner", 0,
        partnerDao.findAll(filter, value, User.ROLE_STUDENT, option).size());
  }

  @Test
  public void testCreateTC3() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals("The address inserted is not the same one",
        addressDao.findById(partnerDto.getAddress().getId()), partnerDto.getAddress());
  }

  @Test
  public void testCreateTC4() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals("The partner inserted is the one given", partnerDao.findById(partnerDto.getId()),
        partnerDto);
  }

  @Test
  public void testCreateTC5() {
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_STUDENT);
    assertEquals("The partner inserted is the one given", partnerDao.findById(partnerDto.getId()),
        partnerDto);
  }

  @Test
  public void testShowAllTC1() {
    String filter = "all";
    String option = "BIN";
    String value = "";
    partnerDto.setStatus(false);
    partnerUcc.create(partnerDto, User.ROLE_PROFESSOR);
    assertEquals("There should be only 1 partner", 1,
        partnerDao.findAll(filter, value, User.ROLE_PROFESSOR, option).size());
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
