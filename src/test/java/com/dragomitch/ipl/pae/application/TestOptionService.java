package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerOptionRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerRepository;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
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
public class TestOptionService {

  @Autowired private ApplicationContext context;

  private OptionService optionService;
  private PartnerRepository partnerRepository;
  private PartnerOptionRepository partnerOptionRepository;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    this.optionService = context.getBean(OptionService.class);
    this.partnerRepository = context.getBean(PartnerRepository.class);
    this.partnerOptionRepository = context.getBean(PartnerOptionRepository.class);
  }

  @AfterEach
  public void cleanUp() {
    ((InMemoryPartnerRepository) partnerRepository).empty();
    ((InMemoryPartnerOptionRepository) partnerOptionRepository).empty();
  }

  @Test
  public void testShowAllTC1() {
    assertEquals(5, optionService.showAll().size());
  }

  @Test
  public void testShowAllTC2() {
    // the mock keeps the options in a HashMap: no order to rely on
    List<String> codes = optionService.showAll().stream().map(Option::getCode).sorted().toList();
    assertEquals(List.of("BBM", "BCH", "BDI", "BIM", "BIN"), codes);
  }

  @Test
  public void testFindAllPartnersByOptionTC1() {
    Partner partner = setUpCorrectPartner();
    partnerRepository.create(partner);
    List<Partner> partners = optionService.findAllPartnersByOption("BIN");
    assertEquals(1, partners.size());
  }

  @Test
  public void testFindAllPartnersByOptionTC2() {
    Partner partner = setUpCorrectPartner();
    partner = partnerRepository.create(partner);
    List<Partner> partners = optionService.findAllPartnersByOption("BIN");
    assertEquals(partner.getId(), partners.get(0).getId());
  }

  @Test
  public void testFindAllPartnersByOptionTC3() {
    Partner partner1 = setUpCorrectPartner();
    partnerRepository.create(partner1);
    PartnerOption option = new PartnerOption();
    option.setCode("BIM");
    option.setDepartement("BinDep");
    partnerOptionRepository.create(option, 1);

    Partner partner2 = setUpCorrectPartner();
    partner2.setId(2);
    partnerRepository.create(partner2);
    assertEquals(2, optionService.findAllPartnersByOption("BIN").size());
    assertEquals(1, optionService.findAllPartnersByOption("BIM").size());
    assertEquals(0, optionService.findAllPartnersByOption("BDI").size());
  }

  /**
   * Creates an instance of Partner with correct data.
   *
   * @return a partner ready to be inserted.
   */
  private Partner setUpCorrectPartner() {
    Partner partner = new Partner();
    partner.setId(1);
    partner.setBusinessName("Business partner");
    partner.setEmail("email@email.org");
    partner.setEmployeeCount(3);
    partner.setFullName("Full partner");
    partner.setLegalName("Legal partner");
    partner.setOrganisationType("Partner organization type");
    partner.setPhoneNumber("+32493184140");
    partner.setWebsite("www.partnerwebsite.com");
    Programme programme = new Programme();
    programme.setId(1);
    partner.setProgramme(programme);
    PartnerOption option = new PartnerOption();
    option.setCode("BIN");
    option.setDepartement("BinDep");
    partnerOptionRepository.create(option, 1);
    List<PartnerOption> options = new ArrayList<PartnerOption>();
    options.add(option);
    partner.setOptions(options);
    Address address = new Address();
    address.setCity("Dublin");
    Country country = new Country();
    country.setCountryCode("IR");
    address.setCountry(country);
    address.setNumber("25");
    address.setPostalCode("12345");
    address.setRegion("partner region");
    address.setStreet("baker street");
    partner.setAddress(address);
    return partner;
  }
}
