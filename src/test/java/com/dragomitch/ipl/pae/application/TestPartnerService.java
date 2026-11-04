package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryAddressRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerOptionRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerRepository;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestPartnerService {

  @Autowired private ApplicationContext context;

  private PartnerRepository partnerRepository;
  private PartnerService partnerService;
  private MockDtoFactory mockDtoFactory;
  private Partner partnerDto;
  private AddressRepository addressRepository;
  private PartnerOptionRepository partnerOptionRepository;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    addressRepository = context.getBean(AddressRepository.class);
    partnerRepository = context.getBean(PartnerRepository.class);
    partnerOptionRepository = context.getBean(PartnerOptionRepository.class);
    partnerService = context.getBean(PartnerService.class);
    mockDtoFactory = new MockDtoFactory();
    partnerDto = mockDtoFactory.getPartner();
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryPartnerRepository) partnerRepository).empty();
    ((InMemoryAddressRepository) addressRepository).empty();
    ((InMemoryPartnerOptionRepository) partnerOptionRepository).empty();
  }

  @Test
  public void testAddOptionTC5() {
    assertEquals(
        ErrorCode.UNKNOWN_OPTION,
        Violations.errorCodeOf(() -> {
          Partner partner = partnerRepository.create(partnerDto);
          PartnerOption option = partner.getOptions().get(0);
          option.setCode("ZZZ");
          partnerService.addOption(
              1, option, 1, Role.PROFESSOR); // Il doit exister une option avec le bon OptionCode
        }));
  }

  @Test
  public void testCreateTC1() {
    assertThrows(
        InsufficientPermissionException.class,
        () -> {
          partnerDto.setOfficial(true);
          partnerService.create(partnerDto, Role.STUDENT);
        });
  }

  @Test
  public void testCreateTC2() {
    PartnerFilter filter = PartnerFilter.ALL;
    String option = "BIN";
    String value = "";
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    assertEquals(
        0,
        partnerRepository.findAll(filter, value, Role.STUDENT, option).size(),
        "There should be only 1 partner");
  }

  @Test
  public void testCreateTC3() {
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    assertEquals(
        addressRepository.findById(partnerDto.getAddress().getId()).orElse(null),
        partnerDto.getAddress(),
        "The address inserted is not the same one");
  }

  @Test
  public void testCreateTC4() {
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    assertEquals(
        partnerRepository.findById(partnerDto.getId()).orElse(null),
        partnerDto,
        "The partner inserted is the one given");
  }

  @Test
  public void testCreateTC5() {
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    assertEquals(
        partnerRepository.findById(partnerDto.getId()).orElse(null),
        partnerDto,
        "The partner inserted is the one given");
  }

  @Test
  public void testShowAllTC1() {
    PartnerFilter filter = PartnerFilter.ALL;
    String option = "BIN";
    String value = "";
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.PROFESSOR);
    assertEquals(
        1,
        partnerRepository.findAll(filter, value, Role.PROFESSOR, option).size(),
        "There should be only 1 partner");
  }

  @Test
  public void testShowAllTC2() {
    PartnerFilter filter = PartnerFilter.ALL;
    String option = "BIM";
    String value = "";
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    assertEquals(0, partnerRepository.findAll(filter, value, Role.STUDENT, option).size());
  }

  @Test
  public void testShowAllTC3() {
    PartnerFilter filter = PartnerFilter.ALL;
    String option = "BIN";
    String value = "";
    partnerDto.setOfficial(true);
    partnerService.create(partnerDto, Role.PROFESSOR);
    assertEquals(1, partnerRepository.findAll(filter, value, Role.STUDENT, option).size());
  }

  @Test
  public void testShowAllTC4() {
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    Partner partner = mockDtoFactory.getPartner();
    partner.setOfficial(true);
    partnerService.create(partner, Role.PROFESSOR);
    PartnerFilter filter = PartnerFilter.ALL;
    String option = "BIN";
    String value = "";
    assertEquals(2, partnerRepository.findAll(filter, value, Role.PROFESSOR, option).size());
  }

  @Test
  public void testShowAllTC5() {
    partnerDto.setOfficial(false);
    partnerService.create(partnerDto, Role.STUDENT);
    Partner partner = mockDtoFactory.getPartner();
    partner.setOfficial(true);
    partnerService.create(partner, Role.PROFESSOR);
    PartnerFilter filter = PartnerFilter.ALL;
    String option = "BIN";
    String value = "";
    assertEquals(1, partnerRepository.findAll(filter, value, Role.STUDENT, option).size());
  }

  /** Stores a partner directly in the 'database', bypassing the use case (like legacy data). */
  private Partner storeWithoutOptions(boolean archived) {
    Partner partner = mockDtoFactory.getPartner();
    partner.setOptions(new ArrayList<PartnerOption>());
    partner.setAddress(addressRepository.create(partner.getAddress()));
    partner.setArchived(archived);
    return partnerRepository.create(partner);
  }

  @Test
  public void testCreateWithOneOptionStoresIt() {
    partnerDto.setOfficial(false);
    Partner created = partnerService.create(partnerDto, Role.PROFESSOR);
    List<PartnerOption> stored = partnerOptionRepository.findAllOptionsByPartner(created.getId());
    assertEquals(1, stored.size(), "The option given at creation must be stored");
    assertEquals("BIN", stored.get(0).getCode());
  }

  @Test
  public void testEditKeepsExistingOptionsWhenNoneAreSent() {
    Partner created = partnerService.create(partnerDto, Role.PROFESSOR);
    Partner changes = mockDtoFactory.getPartner();
    changes.setOptions(new ArrayList<PartnerOption>());
    changes.setFullName("New full name");
    Partner edited = partnerService.edit(created.getId(), changes, Role.PROFESSOR);
    assertEquals("New full name", edited.getFullName());
    assertEquals(1, partnerOptionRepository.findAllOptionsByPartner(created.getId()).size());
  }

  @Test
  public void testEditDoesNotDuplicateExistingOptions() {
    PartnerOption bdi = mockDtoFactory.getPartnerOption();
    bdi.setCode("BDI");
    partnerDto.getOptions().add(bdi);
    Partner created = partnerService.create(partnerDto, Role.PROFESSOR); // BIN + BDI
    Partner changes = mockDtoFactory.getPartner(); // BIN
    PartnerOption sameBdi = mockDtoFactory.getPartnerOption();
    sameBdi.setCode("BDI");
    changes.getOptions().add(sameBdi);
    changes.getOptions().add(mockDtoFactory.getPartnerOption()); // BIN twice in the request
    partnerService.edit(created.getId(), changes, Role.PROFESSOR);
    assertEquals(
        2,
        partnerOptionRepository.findAllOptionsByPartner(created.getId()).size(),
        "Options already stored must not be inserted again");
  }

  @Test
  public void testEditAddsNewOption() {
    Partner created = partnerService.create(partnerDto, Role.PROFESSOR); // BIN
    Partner changes = mockDtoFactory.getPartner();
    PartnerOption bdi = mockDtoFactory.getPartnerOption();
    bdi.setCode("BDI");
    changes.getOptions().add(bdi);
    partnerService.edit(created.getId(), changes, Role.PROFESSOR);
    assertEquals(2, partnerOptionRepository.findAllOptionsByPartner(created.getId()).size());
  }

  @Test
  public void testEditPartnerWithoutOptionsIsRejectedWhenNoneAreSent() {
    Partner stored = storeWithoutOptions(false);
    Partner changes = mockDtoFactory.getPartner();
    changes.setOptions(new ArrayList<PartnerOption>());
    BusinessException ex =
        assertThrows(
            BusinessException.class,
            () -> partnerService.edit(stored.getId(), changes, Role.PROFESSOR));
    assertEquals(ErrorCode.PARTNER_OPTION_REQUIRED, ex.getErrorCode());
  }

  @Test
  public void testEditWithNullOptionsIsRejectedForPartnerWithoutOptions() {
    Partner stored = storeWithoutOptions(false);
    Partner changes = mockDtoFactory.getPartner();
    changes.setOptions(null);
    BusinessException ex =
        assertThrows(
            BusinessException.class,
            () -> partnerService.edit(stored.getId(), changes, Role.PROFESSOR));
    assertEquals(ErrorCode.PARTNER_OPTION_REQUIRED, ex.getErrorCode());
  }

  @Test
  public void testEditPartnerWithoutOptionsAcceptsANewOption() {
    Partner stored = storeWithoutOptions(false);
    Partner changes = mockDtoFactory.getPartner();
    partnerService.edit(stored.getId(), changes, Role.PROFESSOR);
    assertEquals(1, partnerOptionRepository.findAllOptionsByPartner(stored.getId()).size());
  }

  @Test
  public void testRestorePartnerWithoutOptionsIsRejected() {
    Partner stored = storeWithoutOptions(true);
    BusinessException ex =
        assertThrows(
            BusinessException.class, () -> partnerService.restore(stored.getId(), Role.PROFESSOR));
    assertEquals(ErrorCode.PARTNER_OPTION_REQUIRED, ex.getErrorCode());
    assertTrue(
        partnerRepository.findById(stored.getId()).orElseThrow().isArchived(),
        "The partner must stay archived");
  }

  @Test
  public void testRestorePartnerWithAnOption() {
    Partner stored = storeWithoutOptions(true);
    partnerOptionRepository.create(mockDtoFactory.getPartnerOption(), stored.getId());
    Partner restored = partnerService.restore(stored.getId(), Role.PROFESSOR);
    assertFalse(restored.isArchived());
  }

  @Test
  public void testRestoreNotArchivedPartnerIsRejected() {
    Partner created = partnerService.create(partnerDto, Role.PROFESSOR);
    BusinessException ex =
        assertThrows(
            BusinessException.class, () -> partnerService.restore(created.getId(), Role.PROFESSOR));
    assertEquals(ErrorCode.PARTNER_NOT_ARCHIVED, ex.getErrorCode());
  }
}
