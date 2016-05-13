package ucc;

import static org.junit.Assert.assertEquals;

import business.EntityFactory;
import business.dto.DenialReasonDto;
import business.exceptions.BusinessException;
import business.exceptions.RessourceNotFoundException;
import main.ContextManager;
import main.DependencyManager;
import persistence.DenialReasonDao;
import persistence.mocks.MockDenialReasonDao;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDenialReasonUcc {
  private EntityFactory entityFactory;
  private DenialReasonDao denialReasonDao;
  private DenialReasonUcc denialReasonUcc;
  private MockDtoFactory mockDtoFactory;
  private DenialReasonDto denialReason;


  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
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
  @After
  public void cleanUp() {
    ((MockDenialReasonDao) denialReasonDao).empty();
  }

  @Test
  public void testShowAllTC1() {
    ((MockDenialReasonDao) denialReasonDao).empty();
    assertEquals("The database should be empty", 0, denialReasonUcc.showAll().size());
  }

  @Test
  public void testShowAllTC2() {
    denialReasonUcc.create(denialReason);
    assertEquals("The database should countain 1 denial reason", 1,
        denialReasonUcc.showAll().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateTC1() {
    denialReasonUcc.create(null);
  }

  @Test
  public void testCreateTC2() {
    denialReasonUcc.create(denialReason);
    assertEquals("The database should contain only 1 denialreason", 1,
        denialReasonUcc.showAll().size());
  }

  @Test
  public void testCreateTC3() {
    denialReasonUcc.create(denialReason);
    assertEquals(1, denialReason.getId());
  }


  @Test(expected = BusinessException.class)
  public void testCreateTC4() {
    denialReason.setReason("Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
        + "Fusce quam orci, pharetra finibus porttitor vel, consequat vel arcu. "
        + "Cras tempus consequat lectus id imperdiet. "
        + "Sed tincidunt finibus odio eu condimentum. " + "Pellentesque aliquam placerat risus. "
        + "Phasellus lorem massa, placerat ut quam id volutpat.");
    denialReasonUcc.create(denialReason);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEditTC1() {
    denialReasonUcc.edit(0, denialReason);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEditTC2() {
    denialReasonUcc.edit(1, null);
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testEditTC3() {
    denialReasonUcc.edit(1, denialReason);
  }

  @Test
  public void testEditTC4() {
    denialReasonUcc.create(denialReason);
    String newOne = "Autre soEver";
    denialReason.setReason(newOne);
    denialReasonUcc.edit(1, denialReason);
    assertEquals("The update is not correctly done for the field reason", newOne,
        denialReasonUcc.showAll().get(0).getReason());
  }

  @Test
  public void testEditTC5() {
    denialReasonUcc.create(denialReason);
    DenialReasonDto reason = denialReasonUcc.edit(1, denialReason);
    assertEquals("The update is not correctly done for the version", 1, reason.getVersion());
  }


}
