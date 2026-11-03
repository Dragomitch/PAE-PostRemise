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
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
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

  private EntityFactory entityFactory;
  private MockDtoFactory mockDtoFactory;
  private MobilityChoiceDto mobilityChoice;
  private MobilityChoiceService mobilityChoiceService;
  private MobilityRepository mobilityRepository;
  private MobilityChoiceRepository mobilityChoiceRepository;
  private MobilityDocumentRepository mobilityDocumentRepository;
  private PartnerRepository partnerRepository;
  private PartnerDto partner;
  private UserRepository userRepository;
  private UserDto userProf;
  private UserDto userStud;
  private DenialReasonRepository denialReasonRepository;
  private DenialReasonDto denialReason;
  private CountryDto country;
  private CountryRepository countryRepository;
  // private ProgrammeRepository programmeRepository;
  private static final String CANCELLATION_REASON = "testing purposes, of course";

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    this.entityFactory = context.getBean(EntityFactory.class);
    this.mockDtoFactory = new MockDtoFactory(entityFactory);
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
    userProf = mockDtoFactory.getUser(UserDto.ROLE_PROFESSOR);
    userStud = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
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
    mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCheckDataIntregrityTC2() {
    assertEquals(
        List.of("create.mobilityChoice.mobilityType:Pattern"),
        Violations.thrownBy(() -> {
          mobilityChoice.setMobilityType("4444");
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC3() {
    assertEquals(
        List.of("create.mobilityChoice.mobilityType:NotBlank"),
        Violations.thrownBy(() -> {
          mobilityChoice.setMobilityType(null);
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC5() {
    assertEquals(
        ErrorCode.PROFESSOR_CANNOT_APPLY,
        Violations.errorCodeOf(() -> {
          mobilityChoice.setCountry(null);
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC6() {
    assertEquals(
        List.of("create.mobilityChoice.country.countryCode:NotBlank"),
        Violations.thrownBy(() -> {
          mobilityChoice.getCountry().setCountryCode(null);
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC7() {
    assertEquals(
        List.of("create.mobilityChoice.country.countryCode:Size"),
        Violations.thrownBy(() -> {
          mobilityChoice.getCountry().setCountryCode("555");
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC8() {
    assertEquals(
        List.of("create.mobilityChoice.country.countryCode:Size"),
        Violations.thrownBy(() -> {
          mobilityChoice.getCountry().setCountryCode("LOL");
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC9() {
    mobilityChoice.getCountry().setCountryCode("IE");
    mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCheckDataIntregrityTC10() {
    assertEquals(
        List.of("create.mobilityChoice.programme:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoice.setProgramme(null);
          mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC11() {
    assertEquals(
        ErrorCode.UNKNOWN_PROGRAMME,
        Violations.errorCodeOf(() -> {
          mobilityChoice.getProgramme().setId(2);
          mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC12() {
    assertEquals(
        List.of("create.mobilityChoice.user:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoice.setUser(null);
          mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC13() {
    assertEquals(
        ErrorCode.PROFESSOR_CANNOT_APPLY,
        Violations.errorCodeOf(() -> {
          mobilityChoice.getUser().setId(2);
          mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCreateForAnUnknownUser() {
    mobilityChoice.getUser().setId(99);
    assertEquals(
        ErrorCode.UNKNOWN_USER,
        Violations.errorCodeOf(
            () -> mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR)));
  }

  @Test
  public void testCreateInAnUnknownCountry() {
    mobilityChoice.getCountry().setCountryCode("ZZ");
    assertEquals(
        ErrorCode.UNKNOWN_COUNTRY,
        Violations.errorCodeOf(
            () -> mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR)));
  }

  @Test
  public void testCreateTC1() {
    mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCreateTC2() {
    assertEquals(
        List.of("create.mobilityChoice:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoiceService.create(null, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCreateTC3() {
    assertEquals(
        List.of("create.mobilityChoice:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoiceService.create(null, 1, UserDto.ROLE_STUDENT);
        }));
  }

  @Test
  public void testCreateTC4() {
    assertThrows(
        InsufficientPermissionException.class,
        () -> {
          mobilityChoice.setUser(userStud);
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_STUDENT);
        });
  }

  @Test
  public void testCreateTC5() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        });
  }

  @Test
  public void testCreateTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          userProf.setId(5);
          mobilityChoice.setUser(userProf);
          mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        });
  }

  @Test
  public void testCreateTC7() {
    mobilityChoice.setUser(userProf);
    mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    assertEquals(userProf, mobilityChoice.getUser(), "The user is not the one expected");
  }

  @Test
  public void testCreateTC8() {
    mobilityChoice.setUser(userProf);
    MobilityChoiceDto choice =
        mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    assertEquals(choice, mobilityChoice, "The mobility choice is not the one expected");
  }

  @Test
  public void testShowAllTC1() {
    assertEquals(
        List.of("showAll.filter:Pattern"),
        Violations.thrownBy(() -> {
          mobilityChoiceService.showAll(2, UserDto.ROLE_PROFESSOR, "");
        }));
  }

  @Test
  public void testShowAllTC2() {
    assertEquals(
        List.of("showAll.filter:Pattern"),
        Violations.thrownBy(() -> {
          mobilityChoiceService.showAll(2, UserDto.ROLE_PROFESSOR, "LolAdibouLol");
        }));
  }

  @Test
  public void testShowAllTC3() {
    List<MobilityChoiceDto> data = mobilityChoiceService.showAll(2, UserDto.ROLE_PROFESSOR, null);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC4() {
    List<MobilityChoiceDto> data =
        mobilityChoiceService.showAll(
            2, UserDto.ROLE_PROFESSOR, MobilityChoiceRepository.FILTER_REJECTED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC5() {
    List<MobilityChoiceDto> data =
        mobilityChoiceService.showAll(
            2, UserDto.ROLE_PROFESSOR, MobilityChoiceRepository.FILTER_ALL_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC6() {
    List<MobilityChoiceDto> data =
        mobilityChoiceService.showAll(
            2, UserDto.ROLE_PROFESSOR, MobilityChoiceRepository.FILTER_PASSED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC7() {
    List<MobilityChoiceDto> data =
        mobilityChoiceService.showAll(
            1, UserDto.ROLE_PROFESSOR, MobilityChoiceRepository.FILTER_CANCELED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC8() {
    List<MobilityChoiceDto> data =
        mobilityChoiceService.showAll(
            2, UserDto.ROLE_STUDENT, MobilityChoiceRepository.FILTER_CANCELED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testCountAllTC1() {
    int count = mobilityChoiceService.countAll(1, UserDto.ROLE_PROFESSOR, null);
    assertEquals(mobilityChoiceService.showAll(1, UserDto.ROLE_PROFESSOR, null).size(), count);
  }

  @Test
  public void testCountAllTC2() {
    assertEquals(1, mobilityChoiceService.countAll(1, UserDto.ROLE_PROFESSOR, null));
  }

  @Test
  public void testCountAllTC3() {
    assertEquals(1, mobilityChoiceService.countAll(2, UserDto.ROLE_PROFESSOR, null));
  }

  @Test
  public void testCountAllTC4() {
    assertEquals(0, mobilityChoiceService.countAll(2, UserDto.ROLE_STUDENT, null));
  }

  @Test
  public void testCountAllTC5() {
    assertEquals(1, mobilityChoiceService.countAll(1, UserDto.ROLE_STUDENT, null));
  }

  @Test
  public void testCountAllTC6() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_STUDENT);
    assertEquals(2, mobilityChoiceService.countAll(2, UserDto.ROLE_STUDENT, null));
  }

  @Test
  public void testGetMobilityChoiceForUserTC1() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityChoice.setUser(userStud);
          mobilityChoiceService.create(mobilityChoice, 2, UserDto.ROLE_STUDENT);
          mobilityChoiceService.countAll(6, UserDto.ROLE_STUDENT, null);
        });
  }

  @Test
  public void testCancelTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.cancel(-1, userStud.getId(), CANCELLATION_REASON);
        });
  }

  @Test
  public void testCancelTC2() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.cancel(0, userStud.getId(), CANCELLATION_REASON);
        });
  }

  @Test
  public void testCancelTC3() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.cancel(1, userStud.getId(), null);
        });
  }

  @Test
  public void testCancelTC4() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.cancel(1, userStud.getId(), "");
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
          MobilityDto mobility = mockDtoFactory.getMobility();
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

  @Test
  public void testRejectTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.reject(0, denialReason.getId());
        });
  }

  @Test
  public void testRejectTC2() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.reject(mobilityChoice.getId(), 0);
        });
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
    DenialReasonDto withoutText = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
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

  @Test
  public void testConfirmTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.confirm(0, userStud.getId());
        });
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
          MobilityDto mobility = mockDtoFactory.getMobility();
          mobilityRepository.create(mobility);
          mobilityChoiceService.confirm(mobilityChoice.getId(), userStud.getId());
        });
  }

  @Test
  public void testConfirmTC7() {
    MobilityChoiceDto secondaryMobilityChoice = mockDtoFactory.getMobilityChoice();
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
    for (MobilityChoiceDto mobilityChoice :
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
    MobilityChoiceDto cancelled = mockDtoFactory.getMobilityChoice();
    cancelled.setUser(userStud);
    cancelled.setCancellationReason(CANCELLATION_REASON);
    mobilityChoiceRepository.create(cancelled);
    MobilityChoiceDto open = mockDtoFactory.getMobilityChoice();
    open.setUser(userStud);
    mobilityChoiceRepository.create(open);
    mobilityChoice.setUser(userStud);

    mobilityChoiceService.confirm(mobilityChoice.getId(), userProf.getId());

    assertNotNull(mobilityRepository.findById(mobilityChoice.getId()));
    assertNull(cancelled.getDenialReason(), "the cancelled choice is not rejected on top");
    assertEquals(CANCELLATION_REASON, cancelled.getCancellationReason());
    assertNotNull(open.getDenialReason(), "the other open choice of the term is rejected");
  }

  @Test
  public void testConfirmWithNewPartnerTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityChoiceService.confirmWithNewPartner(
              0, partner, userProf.getId(), userProf.getRole());
        });
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
          partner.setStatus(true); // official
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
          MobilityDto mobility = mockDtoFactory.getMobility();
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
          partner.setStatus(false);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerTC9() {
    assertThrows(
        BusinessException.class,
        () -> {
          partner.setStatus(false);
          mobilityChoice.setCountry(country);
          ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
          programme.setId(3);
          mobilityChoice.setProgramme(programme);
          mobilityChoiceService.confirmWithNewPartner(
              mobilityChoice.getId(), partner, userProf.getId(), userProf.getRole());
        });
  }

  @Test
  public void testConfirmWithNewPartnerWithoutOptionIsRejected() {
    PartnerDto newPartner = mockDtoFactory.getPartner(); // id 0: a partner to create
    newPartner.setStatus(false);
    // in the country of the choice (IE)
    newPartner.getAddress().setCountry(mobilityChoice.getCountry());
    newPartner.setOptions(new ArrayList<PartnerOptionDto>());
    // the partner is created by PartnerService.create, whose validation requires an option
    assertEquals(
        List.of("create.partner.options:NotEmpty"),
        Violations.thrownBy(() ->
            mobilityChoiceService.confirmWithNewPartner(
                mobilityChoice.getId(), newPartner, userProf.getId(), userProf.getRole())),
        "The partner must be rejected because it has no option");
    assertNull(
        mobilityRepository.findById(mobilityChoice.getId()),
        "The mobility choice must stay unconfirmed");
  }

  @Test
  public void testConfirmWithNewPartnerWithOption() {
    PartnerDto newPartner = mockDtoFactory.getPartner(); // id 0, one BIN option, in GB
    newPartner.setStatus(false);
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
    PartnerDto newPartner = mockDtoFactory.getPartner(); // in GB
    newPartner.setStatus(false);
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
    PartnerDto newPartner = mockDtoFactory.getPartner(); // in GB
    newPartner.setStatus(false);
    mobilityChoice.setCountry(null);

    mobilityChoiceService.confirmWithNewPartner(
        mobilityChoice.getId(), newPartner, userProf.getId(), userProf.getRole());

    assertEquals("GB", mobilityChoice.getCountry().getCountryCode());
    assertNotNull(mobilityRepository.findById(mobilityChoice.getId()));
  }
}
