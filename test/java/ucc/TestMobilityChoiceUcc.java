package java.ucc;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.context.ErrorManager;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockDenialReasonDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;

import java.util.Map;

public class TestMobilityChoiceUcc {

  private EntityFactory entityFactory;
  private MockDtoFactory mockDtoFactory;
  private MobilityChoiceDto mobilityChoice;
  private MobilityChoiceUcc mobilityChoiceUcc;
  private MobilityDao mobilityDao;
  private MobilityChoiceDao mobilityChoiceDao;
  private MobilityDocumentDao mobilityDocumentDao;
  private PartnerDao partnerDao;
  private PartnerDto partner;
  private UserDao userDao;
  private UserDto userProf;
  private UserDto userStud;
  private DenialReasonDao denialReasonDao;
  private DenialReasonDto denialReason;
  private CountryDto country;
  private CountryDao countryDao;
  // private ProgrammeDao programmeDao;
  private static final String CANCELLATION_REASON = "testing purposes, of course";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
  public void setUp() {
    this.entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.mockDtoFactory = new MockDtoFactory(entityFactory);
    this.mobilityChoiceUcc = DependencyManager.getInstance(MobilityChoiceUcc.class);
    this.mobilityDao = DependencyManager.getInstance(MobilityDao.class);
    this.mobilityChoiceDao = DependencyManager.getInstance(MobilityChoiceDao.class);
    this.mobilityDocumentDao = DependencyManager.getInstance(MobilityDocumentDao.class);
    this.partnerDao = DependencyManager.getInstance(PartnerDao.class);
    this.userDao = DependencyManager.getInstance(UserDao.class);
    this.denialReasonDao = DependencyManager.getInstance(DenialReasonDao.class);
    this.countryDao = DependencyManager.getInstance(CountryDao.class);
    // this.programmeDao = DependencyManager.getInstance(ProgrammeDao.class);
    ErrorManager.load();
    mobilityChoice = mockDtoFactory.getMobilityChoice();
    mobilityChoiceDao.create(mobilityChoice);
    partner = mockDtoFactory.getPartner();
    partnerDao.create(partner);
    userProf = mockDtoFactory.getUser(UserDto.ROLE_PROFESSOR);
    userStud = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
    userDao.create(userProf);
    userDao.create(userStud);
    denialReason = mockDtoFactory.getDenialReason();
    denialReasonDao.create(denialReason);
    country = countryDao.findById("GB");
  }

  /**
   * Cleans up the 'database'.
   */
  @After
  public void cleanUp() {
    ((MockMobilityChoiceDao) mobilityChoiceDao).empty();
    ((MockMobilityDao) mobilityDao).empty();
    ((MockMobilityDocumentDao) mobilityDocumentDao).empty();
    ((MockPartnerDao) partnerDao).empty();
    ((MockDenialReasonDao) denialReasonDao).empty();
    ((MockUserDao) userDao).empty();
  }

  @Test
  public void testCheckDataIntregrityTC1() {
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }


  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC2() {
    mobilityChoice.setMobilityType("4444");
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC3() {
    mobilityChoice.setMobilityType(null);
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC5() {
    mobilityChoice.setCountry(null);
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC6() {
    mobilityChoice.getCountry().setCountryCode(null);
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC7() {
    mobilityChoice.getCountry().setCountryCode("555");
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC8() {
    mobilityChoice.getCountry().setCountryCode("LOL");
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCheckDataIntregrityTC9() {
    mobilityChoice.getCountry().setCountryCode("IE");
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC10() {
    mobilityChoice.setProgramme(null);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC11() {
    mobilityChoice.getProgramme().setId(2);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC12() {
    mobilityChoice.setUser(null);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntregrityTC13() {
    mobilityChoice.getUser().setId(2);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCreateTC1() {
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC2() {
    mobilityChoiceUcc.create(null, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC3() {
    mobilityChoiceUcc.create(null, 1, UserDto.ROLE_STUDENT);
  }

  @Test(expected = InsufficientPermissionException.class)
  public void testCreateTC4() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_STUDENT);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC5() {
    mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC6() {
    userProf.setId(5);
    mobilityChoice.setUser(userProf);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCreateTC7() {
    mobilityChoice.setUser(userProf);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    assertEquals("The user is not the one expected", userProf, mobilityChoice.getUser());
  }

  @Test
  public void testCreateTC8() {
    mobilityChoice.setUser(userProf);
    MobilityChoiceDto choice = mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    assertEquals("The mobility choice is not the one expected", choice, mobilityChoice);
  }

  @Test(expected = BusinessException.class)
  public void testShowAllTC1() {
    mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR, "");
  }

  @Test(expected = BusinessException.class)
  public void testShowAllTC2() {
    mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR, "LolAdibouLol");
  }

  @Test
  public void testShowAllTC3() {
    Map<String, Object> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR, null);
    assertEquals(1, data.size());
  }

  @Test
  public void testShowAllTC4() {
    Map<String, Object> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_REJECTED_MOBILITIES_CHOICES);
    assertEquals(1, data.size());
  }

  @Test
  public void testShowAllTC5() {
    Map<String, Object> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_ALL_MOBILITIES_CHOICES);
    assertEquals(1, data.size());
  }

  @Test
  public void testShowAllTC6() {
    Map<String, Object> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_PASSED_MOBILITIES_CHOICES);
    assertEquals(1, data.size());
  }

  @Test
  public void testShowAllTC7() {
    Map<String, Object> data = mobilityChoiceUcc.showAll(1, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES);
    assertEquals(1, data.size());
  }

  @Test
  public void testShowAllTC8() {
    Map<String, Object> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_STUDENT,
        MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES);
    assertEquals(1, data.size());
  }

  @Test
  public void testCountAllTC1() {
    Map<String, Object> data = mobilityChoiceUcc.countAll(1, UserDto.ROLE_PROFESSOR, null);
    assertEquals(1, data.size());
  }

  @Test
  public void testCountAllTC2() {
    Map<String, Object> data = mobilityChoiceUcc.countAll(1, UserDto.ROLE_PROFESSOR, null);
    assertEquals(1, data.get("count"));
  }

  @Test
  public void testCountAllTC3() {
    Map<String, Object> data = mobilityChoiceUcc.countAll(2, UserDto.ROLE_PROFESSOR, null);
    assertEquals(1, data.get("count"));
  }

  @Test
  public void testCountAllTC4() {
    Map<String, Object> data = mobilityChoiceUcc.countAll(2, UserDto.ROLE_STUDENT, null);
    assertEquals(0, data.get("count"));
  }

  @Test
  public void testCountAllTC5() {
    Map<String, Object> data = mobilityChoiceUcc.countAll(1, UserDto.ROLE_STUDENT, null);
    assertEquals(1, data.get("count"));
  }

  @Test
  public void testCountAllTC6() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_STUDENT);
    Map<String, Object> data = mobilityChoiceUcc.countAll(2, UserDto.ROLE_STUDENT, null);
    assertEquals(2, data.get("count"));
  }

  @Test(expected = BusinessException.class)
  public void testGetMobilityChoiceForUserTC1() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_STUDENT);
    mobilityChoiceUcc.countAll(6, UserDto.ROLE_STUDENT, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC1() {
    mobilityChoiceUcc.cancel(-1, userStud.getId(), CANCELLATION_REASON);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC2() {
    mobilityChoiceUcc.cancel(0, userStud.getId(), CANCELLATION_REASON);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC3() {
    mobilityChoiceUcc.cancel(1, userStud.getId(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCancelTC4() {
    mobilityChoiceUcc.cancel(1, userStud.getId(), "");
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testCancelTC5() {
   * mobilityChoiceUcc.cancel(5, userStud.getId(), CANCELLATION_REASON); }
   */

  @Test(expected = InsufficientPermissionException.class)
  public void testCancelTC6() {
    mobilityChoice.setUser(userProf);
    mobilityChoiceUcc.cancel(mobilityChoice.getId(), userStud.getId(), CANCELLATION_REASON);
  }

  @Test(expected = BusinessException.class)
  public void testCancelTC7() {
    mobilityChoice.setUser(userProf);
    mobilityChoice.setCancellationReason(CANCELLATION_REASON);
    mobilityChoiceUcc.cancel(mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
  }

  @Test(expected = BusinessException.class)
  public void testCancelTC8() {
    mobilityChoice.setUser(userProf);
    mobilityChoice.setDenialReason(denialReason);
    mobilityChoiceUcc.cancel(mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
  }

  @Test(expected = BusinessException.class)
  public void testCancelTC9() {
    mobilityChoice.setUser(userProf);
    MobilityDto mobility = mockDtoFactory.getMobility();
    mobilityDao.create(mobility);
    mobilityChoiceUcc.cancel(mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
  }

  @Test
  public void testCancelTC10() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.cancel(mobilityChoice.getId(), userStud.getId(), CANCELLATION_REASON);
    assertEquals(CANCELLATION_REASON, mobilityChoice.getCancellationReason());
    assertEquals(2, mobilityChoice.getVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectTC1() {
    mobilityChoiceUcc.reject(0, denialReason.getId());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectTC2() {
    mobilityChoiceUcc.reject(mobilityChoice.getId(), 0);
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testRejectTC3() {
   * mobilityChoiceUcc.reject(3, denialReason.getId()); }
   */

  @Test(expected = BusinessException.class)
  public void testRejectTC4() {
    mobilityChoice.setDenialReason(denialReason);
    mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
  }

  @Test(expected = BusinessException.class)
  public void testRejectTC5() {
    mobilityChoice.setCancellationReason(CANCELLATION_REASON);
    mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
  }

  @Test(expected = BusinessException.class)
  public void testRejectTC6() {
    mobilityChoiceUcc.reject(mobilityChoice.getId(), 5);
  }

  @Test(expected = BusinessException.class)
  public void testRejectTC7() {
    mobilityDao.create(mockDtoFactory.getMobility());
    mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
  }

  @Test
  public void testRejectTC8() {
    mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
    assertEquals(2, mobilityChoice.getVersion());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfirmTC1() {
    mobilityChoiceUcc.confirm(0, userStud.getId());
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testConfirmTC2() {
   * mobilityChoiceUcc.confirm(5, userStud.getId()); }
   */

  @Test(expected = BusinessException.class)
  public void testConfirmTC3() {
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), 5);
  }

  @Test(expected = BusinessException.class)
  public void testConfirmTC4() {
    mobilityChoice.setDenialReason(denialReason);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), userStud.getId());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmTC5() {
    mobilityChoice.setCancellationReason(CANCELLATION_REASON);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), userStud.getId());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmTC6() {
    mobilityChoice.setUser(userProf);
    MobilityDto mobility = mockDtoFactory.getMobility();
    mobilityDao.create(mobility);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), userStud.getId());
  }

  @Test
  public void testConfirmTC7() {
    MobilityChoiceDto secondaryMobilityChoice = mockDtoFactory.getMobilityChoice();
    secondaryMobilityChoice.setUser(userStud);
    mobilityChoiceDao.create(secondaryMobilityChoice);
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), userProf.getId());
    assertEquals("It should exist 1 mobility", 1, mobilityDao.findAll().size());
    assertEquals("It should exist 6 documents for that mobility", 8,
        mobilityDocumentDao.findAllByMobility(mobilityChoice.getId()).size());
    int countNotRejected = 0;
    for (MobilityChoiceDto mobilityChoice : mobilityChoiceDao
        .findByUser(mobilityChoice.getUser().getId())) {
      if (mobilityChoice.getDenialReason() != null) {
        countNotRejected++;
      }
    }
    assertEquals("It should remain only 1 mobilityChoice not rejected for that user", 1,
        countNotRejected);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConfirmWithNewPartnerTC1() {
    mobilityChoiceUcc.confirmWithNewPartner(0, partner, userProf.getId(), userProf.getRole());
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testConfirmWithNewPartnerTC2() {
   * mobilityChoiceUcc.confirmWithNewPartner(5, partner, userProf.getId(), userProf.getRole()); }
   */

  @Test(expected = BusinessException.class)
  public void testConfirmWithNewPartnerTC3() {
    mobilityChoice.setDenialReason(denialReason);
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
        userProf.getRole());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmWithNewPartnerTC4() {
    mobilityChoice.setCancellationReason(CANCELLATION_REASON);
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
        userProf.getRole());
  }

  @Test(expected = InsufficientPermissionException.class)
  public void testConfirmWithNewPartnerTC5() {
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userStud.getId(),
        userStud.getRole());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmWithNewPartnerTC6() {
    partner.setStatus(true);// official
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
        userProf.getRole());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmWithNewPartnerTC7() {
    mobilityChoice.setUser(userProf);
    MobilityDto mobility = mockDtoFactory.getMobility();
    mobilityDao.create(mobility);
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
        userProf.getRole());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmWithNewPartnerTC8() {
    partner.setStatus(false);
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
        userProf.getRole());
  }

  @Test(expected = BusinessException.class)
  public void testConfirmWithNewPartnerTC9() {
    partner.setStatus(false);
    mobilityChoice.setCountry(country);
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(3);
    mobilityChoice.setProgramme(programme);
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
        userProf.getRole());
  }

}


