package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryDenialReasonRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityChoiceRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityDocumentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestMobilityChoiceService {

  @Autowired private ApplicationContext context;

  private MockDtoFactory mockDtoFactory;
  private MobilityChoice mobilityChoice;
  private MobilityChoiceService mobilityChoiceService;
  private MobilityRepository mobilityRepository;
  private MobilityChoiceRepository mobilityChoiceRepository;
  private MobilityDocumentRepository mobilityDocumentRepository;
  private PartnerRepository partnerRepository;
  private Partner partner;
  private UserRepository userRepository;
  private User userProf;
  private User userStud;
  private DenialReasonRepository denialReasonRepository;
  private DenialReason denialReason;
  private Country country;
  private CountryRepository countryRepository;
  // private ProgrammeRepository programmeRepository;
  private static final String CANCELLATION_REASON = "testing purposes, of course";

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    this.mockDtoFactory = new MockDtoFactory();
    this.mobilityChoiceService = context.getBean(MobilityChoiceService.class);
    this.mobilityRepository = context.getBean(MobilityRepository.class);
    this.mobilityChoiceRepository = context.getBean(MobilityChoiceRepository.class);
    this.mobilityDocumentRepository = context.getBean(MobilityDocumentRepository.class);
    this.partnerRepository = context.getBean(PartnerRepository.class);
    this.userRepository = context.getBean(UserRepository.class);
    this.denialReasonRepository = context.getBean(DenialReasonRepository.class);
    this.countryRepository = context.getBean(CountryRepository.class);
    // this.programmeRepository = context.getBean(ProgrammeRepository.class);
    mobilityChoice = mockDtoFactory.getMobilityChoice();
    mobilityChoiceRepository.create(mobilityChoice);
    partner = mockDtoFactory.getPartner();
    partnerRepository.create(partner);
    userProf = mockDtoFactory.getUser(Role.PROFESSOR);
    userStud = mockDtoFactory.getUser(Role.STUDENT);
    userRepository.create(userProf);
    userRepository.create(userStud);
    denialReason = mockDtoFactory.getDenialReason();
    denialReasonRepository.create(denialReason);
    country = countryRepository.findById("GB");
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryMobilityChoiceRepository) mobilityChoiceRepository).empty();
    ((InMemoryMobilityRepository) mobilityRepository).empty();
    ((InMemoryMobilityDocumentRepository) mobilityDocumentRepository).empty();
    ((InMemoryPartnerRepository) partnerRepository).empty();
    ((InMemoryDenialReasonRepository) denialReasonRepository).empty();
    ((InMemoryUserRepository) userRepository).empty();
  }

  @Test
  public void testCheckDataIntregrityTC1() {
    mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
  }

  @Test
  public void testCheckDataIntregrityTC5() {
    assertEquals(
        ErrorCode.PROFESSOR_CANNOT_APPLY,
        Violations.errorCodeOf(() -> {
          mobilityChoice.setCountry(null);
          mobilityChoiceService.create(mobilityChoice, 1, Role.PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC9() {
    mobilityChoice.getCountry().setCountryCode("IE");
    mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
  }

  @Test
  public void testCheckDataIntregrityTC11() {
    assertEquals(
        ErrorCode.UNKNOWN_PROGRAMME,
        Violations.errorCodeOf(() -> {
          mobilityChoice.getProgramme().setId(2);
          mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC13() {
    assertEquals(
        ErrorCode.PROFESSOR_CANNOT_APPLY,
        Violations.errorCodeOf(() -> {
          mobilityChoice.getUser().setId(2);
          mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
        }));
  }

  @Test
  public void testCreateForAnUnknownUser() {
    mobilityChoice.getUser().setId(99);
    assertEquals(
        ErrorCode.UNKNOWN_USER,
        Violations.errorCodeOf(
            () -> mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR)));
  }

  @Test
  public void testCreateInAnUnknownCountry() {
    mobilityChoice.getCountry().setCountryCode("ZZ");
    assertEquals(
        ErrorCode.UNKNOWN_COUNTRY,
        Violations.errorCodeOf(
            () -> mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR)));
  }

  @Test
  public void testCreateTC1() {
    mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
  }

  @Test
  public void testCreateTC4() {
    assertThrows(
        InsufficientPermissionException.class,
        () -> {
          mobilityChoice.setUser(userStud);
          mobilityChoiceService.create(mobilityChoice, 1, Role.STUDENT);
        });
  }

  @Test
  public void testCreateTC5() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoiceService.create(mobilityChoice, 1, Role.PROFESSOR);
        });
  }

  @Test
  public void testCreateTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          userProf.setId(5);
          mobilityChoice.setUser(userProf);
          mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
        });
  }

  @Test
  public void testCreateTC7() {
    mobilityChoice.setUser(userProf);
    mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
    assertEquals(userProf, mobilityChoice.getUser(), "The user is not the one expected");
  }

  @Test
  public void testCreateTC8() {
    mobilityChoice.setUser(userProf);
    MobilityChoice choice = mobilityChoiceService.create(mobilityChoice, 2, Role.PROFESSOR);
    assertEquals(choice, mobilityChoice, "The mobility choice is not the one expected");
  }

  @Test
  public void testShowAllTC3() {
    List<MobilityChoice> data =
        mobilityChoiceService.showAll(2, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC4() {
    List<MobilityChoice> data =
        mobilityChoiceService.showAll(2, Role.PROFESSOR, MobilityChoiceFilter.REJECTED);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC5() {
    List<MobilityChoice> data =
        mobilityChoiceService.showAll(2, Role.PROFESSOR, MobilityChoiceFilter.ALL);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC6() {
    List<MobilityChoice> data =
        mobilityChoiceService.showAll(2, Role.PROFESSOR, MobilityChoiceFilter.PASSED);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC7() {
    List<MobilityChoice> data =
        mobilityChoiceService.showAll(1, Role.PROFESSOR, MobilityChoiceFilter.CANCELED);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC8() {
    List<MobilityChoice> data =
        mobilityChoiceService.showAll(2, Role.STUDENT, MobilityChoiceFilter.CANCELED);
    assertNotNull(data);
  }

  @Test
  public void testCountAllTC1() {
    int count = mobilityChoiceService.countAll(1, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE);
    assertEquals(
        mobilityChoiceService.showAll(1, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE).size(),
        count);
  }

  @Test
  public void testCountAllTC2() {
    assertEquals(1, mobilityChoiceService.countAll(1, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE));
  }

  @Test
  public void testCountAllTC3() {
    assertEquals(1, mobilityChoiceService.countAll(2, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE));
  }

  @Test
  public void testCountAllTC4() {
    assertEquals(0, mobilityChoiceService.countAll(2, Role.STUDENT, MobilityChoiceFilter.ACTIVE));
  }

  @Test
  public void testCountAllTC5() {
    assertEquals(1, mobilityChoiceService.countAll(1, Role.STUDENT, MobilityChoiceFilter.ACTIVE));
  }

  @Test
  public void testCountAllTC6() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceService.create(mobilityChoice, 2, Role.STUDENT);
    assertEquals(2, mobilityChoiceService.countAll(2, Role.STUDENT, MobilityChoiceFilter.ACTIVE));
  }

  @Test
  public void testGetMobilityChoiceForUserTC1() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userStud);
          mobilityChoiceService.create(mobilityChoice, 2, Role.STUDENT);
          mobilityChoiceService.countAll(6, Role.STUDENT, MobilityChoiceFilter.ACTIVE);
        });
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testCancelTC5() {
   * mobilityChoiceService.cancel(5, userStud.getId(), CANCELLATION_REASON); }
   */

  @Test
  public void testCancelTC6() {
    assertThrows(
        InsufficientPermissionException.class,
        () -> {
          mobilityChoice.setUser(userProf);
          mobilityChoiceService.cancel(
              mobilityChoice.getId(), userStud.getId(), CANCELLATION_REASON);
        });
  }

  @Test
  public void testCancelTC7() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userProf);
          mobilityChoice.setCancellationReason(CANCELLATION_REASON);
          mobilityChoiceService.cancel(
              mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
        });
  }

  @Test
  public void testCancelTC8() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userProf);
          mobilityChoice.setDenialReason(denialReason);
          mobilityChoiceService.cancel(
              mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
        });
  }

  @Test
  public void testCancelTC9() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userProf);
          Mobility mobility = mockDtoFactory.getMobility();
          mobilityRepository.create(mobility);
          mobilityChoiceService.cancel(
              mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
        });
  }

  @Test
  public void testCancelTC10() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceService.cancel(mobilityChoice.getId(), userStud.getId(), CANCELLATION_REASON);
    assertEquals(CANCELLATION_REASON, mobilityChoice.getCancellationReason());
    assertEquals(2, mobilityChoice.getVersion());
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testRejectTC3() {
   * mobilityChoiceService.reject(3, denialReason.getId()); }
   */

  @Test
  public void testRejectTC4() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setDenialReason(denialReason);
          mobilityChoiceService.reject(mobilityChoice.getId(), denialReason.getId());
        });
  }

  @Test
  public void aChoiceWithADenialReasonCannotBeRejectedAgainWhateverTheReasonText() {
    DenialReason withoutText = new DenialReason();
    withoutText.setId(denialReason.getId());
    mobilityChoice.setDenialReason(withoutText);

    assertEquals(
        ErrorCode.MOBILITY_CHOICE_CLOSED,
        Violations.errorCodeOf(
            () -> mobilityChoiceService.reject(mobilityChoice.getId(), denialReason.getId())));
  }

  @Test
  public void testRejectTC5() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setCancellationReason(CANCELLATION_REASON);
          mobilityChoiceService.reject(mobilityChoice.getId(), denialReason.getId());
        });
  }

  @Test
  public void testRejectTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoiceService.reject(mobilityChoice.getId(), 5);
        });
  }

  @Test
  public void testRejectTC7() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityRepository.create(mockDtoFactory.getMobility());
          mobilityChoiceService.reject(mobilityChoice.getId(), denialReason.getId());
        });
  }

  @Test
  public void testRejectTC8() {
    mobilityChoiceService.reject(mobilityChoice.getId(), denialReason.getId());
    assertEquals(2, mobilityChoice.getVersion());
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testConfirmTC2() {
   * mobilityChoiceService.confirm(5, userStud.getId()); }
   */

  @Test
  public void testConfirmTC3() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoiceService.confirm(mobilityChoice.getId(), 5);
        });
  }

  @Test
  public void testConfirmTC4() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setDenialReason(denialReason);
          mobilityChoiceService.confirm(mobilityChoice.getId(), userStud.getId());
        });
  }

  @Test
  public void testConfirmTC5() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setCancellationReason(CANCELLATION_REASON);
          mobilityChoiceService.confirm(mobilityChoice.getId(), userStud.getId());
        });
  }

  @Test
  public void testConfirmTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userProf);
          Mobility mobility = mockDtoFactory.getMobility();
          mobilityRepository.create(mobility);
          mobilityChoiceService.confirm(mobilityChoice.getId(), userStud.getId());
        });
  }

  @Test
  public void testConfirmTC7() {
    MobilityChoice secondaryMobilityChoice = mockDtoFactory.getMobilityChoice();
    secondaryMobilityChoice.setUser(userStud);
    mobilityChoiceRepository.create(secondaryMobilityChoice);
    mobilityChoice.setUser(userStud);
    mobilityChoiceService.confirm(mobilityChoice.getId(), userProf.getId());
    assertEquals(1, mobilityRepository.findAll().size(), "It should exist 1 mobility");
    assertEquals(
        8,
        mobilityDocumentRepository.findAllByMobility(mobilityChoice.getId()).size(),
        "It should exist 6 documents for that mobility");
    int countNotRejected = 0;
    for (MobilityChoice mobilityChoice :
        mobilityChoiceRepository.findByUser(mobilityChoice.getUser().getId())) {
      if (mobilityChoice.getDenialReason() != null) {
        countNotRejected++;
      }
    }
    assertEquals(
        1, countNotRejected, "It should remain only 1 mobilityChoice not rejected for that user");
  }

  @Test
  public void confirmLeavesTheAlreadyCancelledChoicesOfTheSameTermAlone() {
    MobilityChoice cancelled = mockDtoFactory.getMobilityChoice();
    cancelled.setUser(userStud);
    cancelled.setCancellationReason(CANCELLATION_REASON);
    mobilityChoiceRepository.create(cancelled);
    MobilityChoice open = mockDtoFactory.getMobilityChoice();
    open.setUser(userStud);
    mobilityChoiceRepository.create(open);
    mobilityChoice.setUser(userStud);

    mobilityChoiceService.confirm(mobilityChoice.getId(), userProf.getId());

    assertNotNull(mobilityRepository.findById(mobilityChoice.getId()));
    assertNull(cancelled.getDenialReason(), "the cancelled choice is not rejected on top");
    assertEquals(CANCELLATION_REASON, cancelled.getCancellationReason());
    assertNotNull(open.getDenialReason(), "the other open choice of the term is rejected");
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testConfirmWithNewPartnerTC2() {
   * mobilityChoiceService.confirmWithNewPartner(5, partner, userProf.getId(), userProf.getRole()); }
   */

  @Test
  public void testConfirmWithNewPartnerTC3() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setDenialReason(denialReason);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC4() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setCancellationReason(CANCELLATION_REASON);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC5() {
    assertThrows(
        InsufficientPermissionException.class,
        () -> {
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userStud.getId(), userStud.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          partner.setOfficial(true); // official
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC7() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userProf);
          Mobility mobility = mockDtoFactory.getMobility();
          mobilityRepository.create(mobility);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC8() {
    assertThrows(
        BusinessException.class,
        () -> {
          partner.setOfficial(false);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC9() {
    assertThrows(
        BusinessException.class,
        () -> {
          partner.setOfficial(false);
          mobilityChoice.setCountry(country);
          Programme programme = new Programme();
          programme.setId(3);
          mobilityChoice.setProgramme(programme);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerWithOption() {
    Partner newPartner = mockDtoFactory.getPartner(); // id 0, one BIN option, in GB
    newPartner.setOfficial(false);
    // in the country of the choice (IE)
    newPartner.getAddress().setCountry(mobilityChoice.getCountry());
    mobilityChoiceService.confirmWithNewPartner(
        mobilityChoice.getId(), newPartner, userProf.getId(), userProf.getRole());
    assertNotNull(
        mobilityRepository.findById(mobilityChoice.getId()),
        "The mobility choice must be confirmed");
    assertEquals(newPartner.getId(), mobilityChoice.getPartner().getId());
  }

  @Test
  public void confirmWithNewPartnerRefusesAPartnerOutsideTheChosenCountry() {
    Partner newPartner = mockDtoFactory.getPartner(); // in GB
    newPartner.setOfficial(false);
    mobilityChoice.setCountry(mockDtoFactory.getCountry()); // IE

    assertEquals(
        ErrorCode.COUNTRY_CHANGE_NOT_ALLOWED,
        Violations.errorCodeOf(() ->
            mobilityChoiceService.confirmWithNewPartner(
                mobilityChoice.getId(), newPartner, userProf.getId(), userProf.getRole())));
    assertNull(
        mobilityRepository.findById(mobilityChoice.getId()),
        "The mobility choice must stay unconfirmed");
    assertEquals("IE", mobilityChoice.getCountry().getCountryCode());
  }

  @Test
  public void confirmWithNewPartnerGivesAChoiceWithoutCountryThePartnerCountry() {
    Partner newPartner = mockDtoFactory.getPartner(); // in GB
    newPartner.setOfficial(false);
    mobilityChoice.setCountry(null);

    mobilityChoiceService.confirmWithNewPartner(
        mobilityChoice.getId(), newPartner, userProf.getId(), userProf.getRole());

    assertEquals("GB", mobilityChoice.getCountry().getCountryCode());
    assertNotNull(mobilityRepository.findById(mobilityChoice.getId()));
  }
}
