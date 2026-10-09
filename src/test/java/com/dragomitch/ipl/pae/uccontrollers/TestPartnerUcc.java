package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockAddressDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerOptionDao;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

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
public class TestPartnerUcc {

  @Autowired
  private ApplicationContext context;

  private EntityFactory entityFactory;
  private PartnerDao partnerDao;
  private PartnerUcc partnerUcc;
  private MockDtoFactory mockDtoFactory;
  private PartnerDto partnerDto;
  private AddressDao addressDao;
  private PartnerOptionDao partnerOptionDao;

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    addressDao = context.getBean(AddressDao.class);
    partnerDao = context.getBean(PartnerDao.class);
    partnerOptionDao = context.getBean(PartnerOptionDao.class);
    partnerUcc = context.getBean(PartnerUcc.class);
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
    assertThrows(ConstraintViolationException.class, () -> {
      PartnerOptionDto option = mockDtoFactory.getPartnerOption();
      partnerUcc.addOption(0, option, 1, User.ROLE_PROFESSOR); // L'id doit être > 0
    });
  }

  @Test
  public void testAddOptionTC2() {
    assertThrows(ConstraintViolationException.class, () -> {
      partnerUcc.addOption(1, null, 1, User.ROLE_PROFESSOR); // Le PartnerOption doit être différent de null
    });
  }

  @Test
  public void testAddOptionTC3() {
    assertThrows(ConstraintViolationException.class, () -> {
      PartnerOptionDto option = mockDtoFactory.getPartnerOption();
      option.setCode("");
      partnerUcc.addOption(1, option, 1, User.ROLE_PROFESSOR); // le code doit être une String valide
    });
  }

  @Test
  public void testAddOptionTC4() {
    assertThrows(ConstraintViolationException.class, () -> {
      PartnerOptionDto option = mockDtoFactory.getPartnerOption();
      option.setDepartement("");
      partnerUcc.addOption(1, option, 1, User.ROLE_PROFESSOR); // le departement doit être une String valide
    });
  }

  @Test
  public void testAddOptionTC5() {
    assertEquals(ErrorCode.UNKNOWN_OPTION, Violations.errorCodeOf(() -> {
      PartnerDto partner = partnerDao.create(partnerDto);
      PartnerOptionDto option = partner.getOptions().get(0);
      option.setCode("ZZZ");
      partnerUcc.addOption(1, option, 1, User.ROLE_PROFESSOR); // Il doit exister une option avec le bon OptionCode
    }));
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

  /**
   * Stores a partner directly in the 'database', bypassing the use case (like legacy data).
   */
  private PartnerDto storeWithoutOptions(boolean archived) {
    PartnerDto partner = mockDtoFactory.getPartner();
    partner.setOptions(new ArrayList<PartnerOptionDto>());
    partner.setAddress(addressDao.create(partner.getAddress()));
    partner.setArchived(archived);
    return partnerDao.create(partner);
  }

  @Test
  public void testCreateWithOneOptionStoresIt() {
    partnerDto.setStatus(false);
    PartnerDto created = partnerUcc.create(partnerDto, User.ROLE_PROFESSOR);
    List<PartnerOptionDto> stored = partnerOptionDao.findAllOptionsByPartner(created.getId());
    assertEquals(1, stored.size(), "The option given at creation must be stored");
    assertEquals("BIN", stored.get(0).getCode());
  }

  @Test
  public void testCreateWithEmptyOptionsIsRejected() {
    partnerDto.setStatus(false);
    partnerDto.setOptions(new ArrayList<PartnerOptionDto>());
    assertEquals(List.of("create.partner.options:NotEmpty"),
        Violations.thrownBy(() -> partnerUcc.create(partnerDto, User.ROLE_PROFESSOR)));
    assertNull(partnerDao.findById(1), "No partner must be created without an option");
  }

  @Test
  public void testCreateWithNullOptionsIsRejected() {
    partnerDto.setStatus(false);
    partnerDto.setOptions(null);
    assertEquals(List.of("create.partner.options:NotEmpty"),
        Violations.thrownBy(() -> partnerUcc.create(partnerDto, User.ROLE_STUDENT)));
    assertNull(partnerDao.findById(1), "No partner must be created without an option");
  }

  @Test
  public void testEditKeepsExistingOptionsWhenNoneAreSent() {
    PartnerDto created = partnerUcc.create(partnerDto, User.ROLE_PROFESSOR);
    PartnerDto changes = mockDtoFactory.getPartner();
    changes.setOptions(new ArrayList<PartnerOptionDto>());
    changes.setFullName("New full name");
    PartnerDto edited = partnerUcc.edit(created.getId(), changes, User.ROLE_PROFESSOR);
    assertEquals("New full name", edited.getFullName());
    assertEquals(1, partnerOptionDao.findAllOptionsByPartner(created.getId()).size());
  }

  @Test
  public void testEditDoesNotDuplicateExistingOptions() {
    PartnerOptionDto bdi = mockDtoFactory.getPartnerOption();
    bdi.setCode("BDI");
    partnerDto.getOptions().add(bdi);
    PartnerDto created = partnerUcc.create(partnerDto, User.ROLE_PROFESSOR); // BIN + BDI
    PartnerDto changes = mockDtoFactory.getPartner(); // BIN
    PartnerOptionDto sameBdi = mockDtoFactory.getPartnerOption();
    sameBdi.setCode("BDI");
    changes.getOptions().add(sameBdi);
    changes.getOptions().add(mockDtoFactory.getPartnerOption()); // BIN twice in the request
    partnerUcc.edit(created.getId(), changes, User.ROLE_PROFESSOR);
    assertEquals(2, partnerOptionDao.findAllOptionsByPartner(created.getId()).size(),
        "Options already stored must not be inserted again");
  }

  @Test
  public void testEditWithANullOptionIsAValidationFailure() {
    PartnerDto created = partnerUcc.create(partnerDto, User.ROLE_PROFESSOR); // BIN
    PartnerDto changes = mockDtoFactory.getPartner();
    changes.getOptions().add(null);
    changes.setFullName("Not saved");
    assertEquals(List.of("edit.partner.options[1].<list element>:NotNull"),
        Violations.thrownBy(() -> partnerUcc.edit(created.getId(), changes,
            User.ROLE_PROFESSOR)));
    assertEquals(1, partnerOptionDao.findAllOptionsByPartner(created.getId()).size());
  }

  @Test
  public void testCreateWithANullOptionIsAValidationFailure() {
    partnerDto.getOptions().add(null);
    assertEquals(List.of("create.partner.options[1].<list element>:NotNull"),
        Violations.thrownBy(() -> partnerUcc.create(partnerDto, User.ROLE_PROFESSOR)));
    assertNull(partnerDao.findById(1), "No partner must be created with a null option");
  }

  @Test
  public void testEditAddsNewOption() {
    PartnerDto created = partnerUcc.create(partnerDto, User.ROLE_PROFESSOR); // BIN
    PartnerDto changes = mockDtoFactory.getPartner();
    PartnerOptionDto bdi = mockDtoFactory.getPartnerOption();
    bdi.setCode("BDI");
    changes.getOptions().add(bdi);
    partnerUcc.edit(created.getId(), changes, User.ROLE_PROFESSOR);
    assertEquals(2, partnerOptionDao.findAllOptionsByPartner(created.getId()).size());
  }

  @Test
  public void testEditPartnerWithoutOptionsIsRejectedWhenNoneAreSent() {
    PartnerDto stored = storeWithoutOptions(false);
    PartnerDto changes = mockDtoFactory.getPartner();
    changes.setOptions(new ArrayList<PartnerOptionDto>());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> partnerUcc.edit(stored.getId(), changes, User.ROLE_PROFESSOR));
    assertEquals(ErrorCode.PARTNER_OPTION_REQUIRED, ex.getErrorCode());
  }

  @Test
  public void testEditWithNullOptionsIsRejectedForPartnerWithoutOptions() {
    PartnerDto stored = storeWithoutOptions(false);
    PartnerDto changes = mockDtoFactory.getPartner();
    changes.setOptions(null);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> partnerUcc.edit(stored.getId(), changes, User.ROLE_PROFESSOR));
    assertEquals(ErrorCode.PARTNER_OPTION_REQUIRED, ex.getErrorCode());
  }

  @Test
  public void testEditPartnerWithoutOptionsAcceptsANewOption() {
    PartnerDto stored = storeWithoutOptions(false);
    PartnerDto changes = mockDtoFactory.getPartner();
    partnerUcc.edit(stored.getId(), changes, User.ROLE_PROFESSOR);
    assertEquals(1, partnerOptionDao.findAllOptionsByPartner(stored.getId()).size());
  }

  @Test
  public void testRestorePartnerWithoutOptionsIsRejected() {
    PartnerDto stored = storeWithoutOptions(true);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> partnerUcc.restore(stored.getId(), User.ROLE_PROFESSOR));
    assertEquals(ErrorCode.PARTNER_OPTION_REQUIRED, ex.getErrorCode());
    assertTrue(partnerDao.findById(stored.getId()).isArchived(), "The partner must stay archived");
  }

  @Test
  public void testRestorePartnerWithAnOption() {
    PartnerDto stored = storeWithoutOptions(true);
    partnerOptionDao.create(mockDtoFactory.getPartnerOption(), stored.getId());
    PartnerDto restored = partnerUcc.restore(stored.getId(), User.ROLE_PROFESSOR);
    assertFalse(restored.isArchived());
  }

  @Test
  public void testRestoreNotArchivedPartnerIsRejected() {
    PartnerDto created = partnerUcc.create(partnerDto, User.ROLE_PROFESSOR);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> partnerUcc.restore(created.getId(), User.ROLE_PROFESSOR));
    assertEquals(ErrorCode.PARTNER_NOT_ARCHIVED, ex.getErrorCode());
  }

}
