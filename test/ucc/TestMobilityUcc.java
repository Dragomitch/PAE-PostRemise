package ucc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import business.EntityFactory;
import business.dto.DenialReasonDto;
import business.dto.MobilityChoiceDto;
import business.dto.MobilityDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import business.exceptions.RessourceNotFoundException;
import main.ContextManager;
import main.DependencyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import persistence.MobilityChoiceDao;
import persistence.MobilityDao;
import persistence.UserDao;
import persistence.mocks.MockMobilityChoiceDao;
import persistence.mocks.MockMobilityDao;
import persistence.mocks.MockUserDao;

public class TestMobilityUcc {

  private EntityFactory entityFactory;
  private MockDtoFactory mockDtoFactory;
  private MobilityUcc mobilityUcc;
  private MobilityDao mobilityDao;
  private MobilityChoiceUcc mobilityChoiceUcc;
  private DenialReasonUcc denialReasonUcc;
  private UserUcc userUcc;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    mobilityUcc = DependencyManager.getInstance(MobilityUcc.class);
    mobilityDao = DependencyManager.getInstance(MobilityDao.class);
    mobilityChoiceUcc = DependencyManager.getInstance(MobilityChoiceUcc.class);
    userUcc = DependencyManager.getInstance(UserUcc.class);
    denialReasonUcc = DependencyManager.getInstance(DenialReasonUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    // Creates User

    UserDto user = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
    user.setEmail("Cyka@blyat.ru");
    user.setUsername("CykaBlyat");
    userUcc.signup(user);
    // Creates mobility
    MobilityChoiceDto mobilityChoice = mockDtoFactory.getMobilityChoice();
    mobilityChoice.setUser(user);
    mobilityChoice = mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_STUDENT);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), 1);
  }

  /**
   * Cleans up the 'database'.
   */
  @After
  public void cleanUp() {
    ((MockMobilityDao) mobilityDao).empty();
    MobilityChoiceDao mobilityChoiceDao = DependencyManager.getInstance(MobilityChoiceDao.class);
    ((MockMobilityChoiceDao) mobilityChoiceDao).empty();
    UserDao userDao = DependencyManager.getInstance(UserDao.class);
    ((MockUserDao) userDao).empty();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfirmProEcoEncodingTC1() {
    mobilityUcc.confirmProEcoEncoding(-1, 1);
  }

  @Test(expected = BusinessException.class)
  public void testConfirmProEcoEncodingTC2() {
    MobilityDto mobility = mobilityDao.findById(1);
    mobility.setState(MobilityDto.STATE_CANCELLED);
    mobilityUcc.confirmProEcoEncoding(1, 1);
  }

  @Test
  public void testConfirmProEcoEncodingTC3() {
    mobilityUcc.confirmProEcoEncoding(1, 1);
    MobilityDto mobility = mobilityDao.findById(1);
    assertTrue(mobility.isEncodedInProEco());
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testConfirmProEcoEncodingTC4() {
    mobilityUcc.confirmProEcoEncoding(99, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfirmSecondSoftwareEncodingTC1() {
    mobilityUcc.confirmSecondSoftwareEncoding(0, 1);
  }

  @Test(expected = BusinessException.class)
  public void testConfirmSecondSoftwareEncodingTC2() {
    MobilityDto mobility = mobilityDao.findById(1);
    mobility.setState(MobilityDto.STATE_CANCELLED);
    mobilityUcc.confirmSecondSoftwareEncoding(1, 1);
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC3() {
    mobilityUcc.confirmSecondSoftwareEncoding(1, 1);
    MobilityDto mobility = mobilityDao.findById(1);
    assertTrue(mobility.isEncodedInSecondSoftware());
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testConfirmSecondSoftwareEncodingTC4() {
    mobilityUcc.confirmSecondSoftwareEncoding(99, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC1() {
    mobilityUcc.cancel(-1, 1, null, 1, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC2() {
    mobilityUcc.cancel(1, 1, null, -1, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC3() {
    mobilityUcc.cancel(1, 1, null, 1, 1, UserDto.ROLE_STUDENT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC4() {
    mobilityUcc.cancel(1, 1, null, 1, -1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC5() {
    mobilityUcc.cancel(1, 1, null, 1, 1, null);
  }

  @Test(expected = BusinessException.class)
  public void testCancelTC6() {
    mobilityUcc.cancel(1, 1, null, 99, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test()
  public void testCancelTC7() {
    DenialReasonDto denialReason = denialReasonUcc.create(mockDtoFactory.getDenialReason());
    MobilityDto mobility = mobilityUcc.cancel(1, 1, null, denialReason.getId(), 1, UserDto.ROLE_PROFESSOR);
    assertEquals(MobilityDto.STATE_CANCELLED, mobility.getState());
    assertEquals(denialReason.getId(), mobility.getDenialReason().getId());
  }

  @Test()
  public void testCancelTC8() {
    MobilityDto mobility = mobilityUcc.cancel(1, 1, "Toto 456", 0, 1, UserDto.ROLE_STUDENT);
    assertEquals(MobilityDto.STATE_CANCELLED, mobility.getState());
    assertEquals("Toto 456", mobility.getCancellationReason());
  }

//  @Test(expected = InsufficientPermissionException.class)
//  public void testCancelTC9() {
//    // System.out.println(mobilityDao.findById(1).getNominatedStudent().getId());
//    // // TODO continuer
//    //MobilityDto mobility = mobilityUcc.cancel(1, 1, "Toto 456", 0, 3, UserDto.ROLE_STUDENT);
//  }

}
