package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
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
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;

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
public class TestMobilityChoiceUcc {

  @Autowired
  private ApplicationContext context;

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

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    this.entityFactory = context.getBean(EntityFactory.class);
    this.mockDtoFactory = new MockDtoFactory(entityFactory);
    this.mobilityChoiceUcc = context.getBean(MobilityChoiceUcc.class);
    this.mobilityDao = context.getBean(MobilityDao.class);
    this.mobilityChoiceDao = context.getBean(MobilityChoiceDao.class);
    this.mobilityDocumentDao = context.getBean(MobilityDocumentDao.class);
    this.partnerDao = context.getBean(PartnerDao.class);
    this.userDao = context.getBean(UserDao.class);
    this.denialReasonDao = context.getBean(DenialReasonDao.class);
    this.countryDao = context.getBean(CountryDao.class);
    // this.programmeDao = context.getBean(ProgrammeDao.class);
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
  @AfterEach
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


  @Test
  public void testCheckDataIntregrityTC2() {
    assertEquals(List.of("create.mobilityChoice.mobilityType:Pattern"),
        Violations.thrownBy(() -> {
          mobilityChoice.setMobilityType("4444");
          mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC3() {
    assertEquals(List.of("create.mobilityChoice.mobilityType:NotBlank"),
        Violations.thrownBy(() -> {
          mobilityChoice.setMobilityType(null);
          mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC5() {
    assertEquals(ErrorCode.PROFESSOR_CANNOT_APPLY, Violations.errorCodeOf(() -> {
      mobilityChoice.setCountry(null);
      mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
    }));
  }

  @Test
  public void testCheckDataIntregrityTC6() {
    assertEquals(List.of("create.mobilityChoice.country.countryCode:NotBlank"),
        Violations.thrownBy(() -> {
          mobilityChoice.getCountry().setCountryCode(null);
          mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC7() {
    assertEquals(List.of("create.mobilityChoice.country.countryCode:Size"),
        Violations.thrownBy(() -> {
          mobilityChoice.getCountry().setCountryCode("555");
          mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC8() {
    assertEquals(List.of("create.mobilityChoice.country.countryCode:Size"),
        Violations.thrownBy(() -> {
          mobilityChoice.getCountry().setCountryCode("LOL");
          mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC9() {
    mobilityChoice.getCountry().setCountryCode("IE");
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCheckDataIntregrityTC10() {
    assertEquals(List.of("create.mobilityChoice.programme:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoice.setProgramme(null);
          mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC11() {
    assertEquals(ErrorCode.UNKNOWN_PROGRAMME, Violations.errorCodeOf(() -> {
      mobilityChoice.getProgramme().setId(2);
      mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    }));
  }

  @Test
  public void testCheckDataIntregrityTC12() {
    assertEquals(List.of("create.mobilityChoice.user:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoice.setUser(null);
          mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCheckDataIntregrityTC13() {
    assertEquals(ErrorCode.PROFESSOR_CANNOT_APPLY, Violations.errorCodeOf(() -> {
      mobilityChoice.getUser().setId(2);
      mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    }));
  }

  @Test
  public void testCreateForAnUnknownUser() {
    mobilityChoice.getUser().setId(99);
    assertEquals(ErrorCode.UNKNOWN_USER,
        Violations.errorCodeOf(() -> mobilityChoiceUcc.create(mobilityChoice, 2,
            UserDto.ROLE_PROFESSOR)));
  }

  @Test
  public void testCreateInAnUnknownCountry() {
    mobilityChoice.getCountry().setCountryCode("ZZ");
    assertEquals(ErrorCode.UNKNOWN_COUNTRY,
        Violations.errorCodeOf(() -> mobilityChoiceUcc.create(mobilityChoice, 2,
            UserDto.ROLE_PROFESSOR)));
  }

  @Test
  public void testCreateTC1() {
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
  }

  @Test
  public void testCreateTC2() {
    assertEquals(List.of("create.mobilityChoice:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoiceUcc.create(null, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCreateTC3() {
    assertEquals(List.of("create.mobilityChoice:NotNull"),
        Violations.thrownBy(() -> {
          mobilityChoiceUcc.create(null, 1, UserDto.ROLE_STUDENT);
        }));
  }

  @Test
  public void testCreateTC4() {
    assertThrows(InsufficientPermissionException.class, () -> {
      mobilityChoice.setUser(userStud);
      mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_STUDENT);
    });
  }

  @Test
  public void testCreateTC5() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCreateTC6() {
    assertThrows(BusinessException.class, () -> {
      userProf.setId(5);
      mobilityChoice.setUser(userProf);
      mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCreateTC7() {
    mobilityChoice.setUser(userProf);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    assertEquals(userProf, mobilityChoice.getUser(), "The user is not the one expected");
  }

  @Test
  public void testCreateTC8() {
    mobilityChoice.setUser(userProf);
    MobilityChoiceDto choice = mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_PROFESSOR);
    assertEquals(choice, mobilityChoice, "The mobility choice is not the one expected");
  }

  @Test
  public void testShowAllTC1() {
    assertEquals(List.of("showAll.filter:Pattern"),
        Violations.thrownBy(() -> {
          mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR, "");
        }));
  }

  @Test
  public void testShowAllTC2() {
    assertEquals(List.of("showAll.filter:Pattern"),
        Violations.thrownBy(() -> {
          mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR, "LolAdibouLol");
        }));
  }

  @Test
  public void testShowAllTC3() {
    List<MobilityChoiceDto> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR, null);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC4() {
    List<MobilityChoiceDto> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_REJECTED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC5() {
    List<MobilityChoiceDto> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_ALL_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC6() {
    List<MobilityChoiceDto> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_PASSED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC7() {
    List<MobilityChoiceDto> data = mobilityChoiceUcc.showAll(1, UserDto.ROLE_PROFESSOR,
        MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testShowAllTC8() {
    List<MobilityChoiceDto> data = mobilityChoiceUcc.showAll(2, UserDto.ROLE_STUDENT,
        MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES);
    assertNotNull(data);
  }

  @Test
  public void testCountAllTC1() {
    int count = mobilityChoiceUcc.countAll(1, UserDto.ROLE_PROFESSOR, null);
    assertEquals(mobilityChoiceUcc.showAll(1, UserDto.ROLE_PROFESSOR, null).size(), count);
  }

  @Test
  public void testCountAllTC2() {
    assertEquals(1, mobilityChoiceUcc.countAll(1, UserDto.ROLE_PROFESSOR, null));
  }

  @Test
  public void testCountAllTC3() {
    assertEquals(1, mobilityChoiceUcc.countAll(2, UserDto.ROLE_PROFESSOR, null));
  }

  @Test
  public void testCountAllTC4() {
    assertEquals(0, mobilityChoiceUcc.countAll(2, UserDto.ROLE_STUDENT, null));
  }

  @Test
  public void testCountAllTC5() {
    assertEquals(1, mobilityChoiceUcc.countAll(1, UserDto.ROLE_STUDENT, null));
  }

  @Test
  public void testCountAllTC6() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_STUDENT);
    assertEquals(2, mobilityChoiceUcc.countAll(2, UserDto.ROLE_STUDENT, null));
  }

  @Test
  public void testGetMobilityChoiceForUserTC1() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setUser(userStud);
      mobilityChoiceUcc.create(mobilityChoice, 2, UserDto.ROLE_STUDENT);
      mobilityChoiceUcc.countAll(6, UserDto.ROLE_STUDENT, null);
    });
  }

  @Test
  public void testCancelTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.cancel(-1, userStud.getId(), CANCELLATION_REASON);
    });
  }

  @Test
  public void testCancelTC2() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.cancel(0, userStud.getId(), CANCELLATION_REASON);
    });
  }

  @Test
  public void testCancelTC3() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.cancel(1, userStud.getId(), null);
    });
  }

  @Test
  public void testCancelTC4() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.cancel(1, userStud.getId(), "");
    });
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testCancelTC5() {
   * mobilityChoiceUcc.cancel(5, userStud.getId(), CANCELLATION_REASON); }
   */

  @Test
  public void testCancelTC6() {
    assertThrows(InsufficientPermissionException.class, () -> {
      mobilityChoice.setUser(userProf);
      mobilityChoiceUcc.cancel(mobilityChoice.getId(), userStud.getId(), CANCELLATION_REASON);
    });
  }

  @Test
  public void testCancelTC7() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setUser(userProf);
      mobilityChoice.setCancellationReason(CANCELLATION_REASON);
      mobilityChoiceUcc.cancel(mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
    });
  }

  @Test
  public void testCancelTC8() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setUser(userProf);
      mobilityChoice.setDenialReason(denialReason);
      mobilityChoiceUcc.cancel(mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
    });
  }

  @Test
  public void testCancelTC9() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setUser(userProf);
      MobilityDto mobility = mockDtoFactory.getMobility();
      mobilityDao.create(mobility);
      mobilityChoiceUcc.cancel(mobilityChoice.getId(), userProf.getId(), CANCELLATION_REASON);
    });
  }

  @Test
  public void testCancelTC10() {
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.cancel(mobilityChoice.getId(), userStud.getId(), CANCELLATION_REASON);
    assertEquals(CANCELLATION_REASON, mobilityChoice.getCancellationReason());
    assertEquals(2, mobilityChoice.getVersion());
  }

  @Test
  public void testRejectTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.reject(0, denialReason.getId());
    });
  }

  @Test
  public void testRejectTC2() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.reject(mobilityChoice.getId(), 0);
    });
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testRejectTC3() {
   * mobilityChoiceUcc.reject(3, denialReason.getId()); }
   */

  @Test
  public void testRejectTC4() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setDenialReason(denialReason);
      mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
    });
  }

  @Test
  public void testRejectTC5() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setCancellationReason(CANCELLATION_REASON);
      mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
    });
  }

  @Test
  public void testRejectTC6() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoiceUcc.reject(mobilityChoice.getId(), 5);
    });
  }

  @Test
  public void testRejectTC7() {
    assertThrows(BusinessException.class, () -> {
      mobilityDao.create(mockDtoFactory.getMobility());
      mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
    });
  }

  @Test
  public void testRejectTC8() {
    mobilityChoiceUcc.reject(mobilityChoice.getId(), denialReason.getId());
    assertEquals(2, mobilityChoice.getVersion());
  }

  @Test
  public void testConfirmTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.confirm(0, userStud.getId());
    });
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testConfirmTC2() {
   * mobilityChoiceUcc.confirm(5, userStud.getId()); }
   */

  @Test
  public void testConfirmTC3() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoiceUcc.confirm(mobilityChoice.getId(), 5);
    });
  }

  @Test
  public void testConfirmTC4() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setDenialReason(denialReason);
      mobilityChoiceUcc.confirm(mobilityChoice.getId(), userStud.getId());
    });
  }

  @Test
  public void testConfirmTC5() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setCancellationReason(CANCELLATION_REASON);
      mobilityChoiceUcc.confirm(mobilityChoice.getId(), userStud.getId());
    });
  }

  @Test
  public void testConfirmTC6() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setUser(userProf);
      MobilityDto mobility = mockDtoFactory.getMobility();
      mobilityDao.create(mobility);
      mobilityChoiceUcc.confirm(mobilityChoice.getId(), userStud.getId());
    });
  }

  @Test
  public void testConfirmTC7() {
    MobilityChoiceDto secondaryMobilityChoice = mockDtoFactory.getMobilityChoice();
    secondaryMobilityChoice.setUser(userStud);
    mobilityChoiceDao.create(secondaryMobilityChoice);
    mobilityChoice.setUser(userStud);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), userProf.getId());
    assertEquals(1, mobilityDao.findAll().size(), "It should exist 1 mobility");
    assertEquals(8, mobilityDocumentDao.findAllByMobility(mobilityChoice.getId()).size(), "It should exist 6 documents for that mobility");
    int countNotRejected = 0;
    for (MobilityChoiceDto mobilityChoice : mobilityChoiceDao
        .findByUser(mobilityChoice.getUser().getId())) {
      if (mobilityChoice.getDenialReason() != null) {
        countNotRejected++;
      }
    }
    assertEquals(1, countNotRejected, "It should remain only 1 mobilityChoice not rejected for that user");
  }

  @Test
  public void testConfirmWithNewPartnerTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      mobilityChoiceUcc.confirmWithNewPartner(0, partner, userProf.getId(), userProf.getRole());
    });
  }

  // TODO
  /*
   * @Test(expected = BusinessException.class) public void testConfirmWithNewPartnerTC2() {
   * mobilityChoiceUcc.confirmWithNewPartner(5, partner, userProf.getId(), userProf.getRole()); }
   */

  @Test
  public void testConfirmWithNewPartnerTC3() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setDenialReason(denialReason);
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
          userProf.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerTC4() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setCancellationReason(CANCELLATION_REASON);
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
          userProf.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerTC5() {
    assertThrows(InsufficientPermissionException.class, () -> {
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userStud.getId(),
          userStud.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerTC6() {
    assertThrows(BusinessException.class, () -> {
      partner.setStatus(true);// official
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
          userProf.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerTC7() {
    assertThrows(BusinessException.class, () -> {
      mobilityChoice.setUser(userProf);
      MobilityDto mobility = mockDtoFactory.getMobility();
      mobilityDao.create(mobility);
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
          userProf.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerTC8() {
    assertThrows(BusinessException.class, () -> {
      partner.setStatus(false);
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
          userProf.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerTC9() {
    assertThrows(BusinessException.class, () -> {
      partner.setStatus(false);
      mobilityChoice.setCountry(country);
      ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
      programme.setId(3);
      mobilityChoice.setProgramme(programme);
      mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), partner, userProf.getId(),
          userProf.getRole());
    });
  }

  @Test
  public void testConfirmWithNewPartnerWithoutOptionIsRejected() {
    PartnerDto newPartner = mockDtoFactory.getPartner(); // id 0: a partner to create
    newPartner.setStatus(false);
    newPartner.setOptions(new ArrayList<PartnerOptionDto>());
    // the partner is created by PartnerUcc.create, whose validation requires an option
    assertEquals(List.of("create.partner.options:NotEmpty"),
        Violations.thrownBy(() -> mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(),
            newPartner, userProf.getId(), userProf.getRole())),
        "The partner must be rejected because it has no option");
    assertNull(mobilityDao.findById(mobilityChoice.getId()), "The mobility choice must stay unconfirmed");
  }

  @Test
  public void testConfirmWithNewPartnerWithOption() {
    PartnerDto newPartner = mockDtoFactory.getPartner(); // id 0, one BIN option
    newPartner.setStatus(false);
    mobilityChoiceUcc.confirmWithNewPartner(mobilityChoice.getId(), newPartner, userProf.getId(),
        userProf.getRole());
    assertNotNull(mobilityDao.findById(mobilityChoice.getId()), "The mobility choice must be confirmed");
  }

}


