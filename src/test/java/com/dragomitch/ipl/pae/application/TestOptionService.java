package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerOptionRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPartnerRepository;
import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Partner;
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

  @Autowired
  private ApplicationContext context;

  private EntityFactory entityFactory;
  private OptionService optionService;
  private PartnerRepository partnerRepository;
  private PartnerOptionRepository partnerOptionRepository;

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    this.entityFactory = context.getBean(EntityFactory.class);
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
    List<String> codes = optionService.showAll().stream().map(OptionDto::getCode).sorted().toList();
    assertEquals(List.of("BBM", "BCH", "BDI", "BIM", "BIN"), codes);
  }

  @Test
  public void testFindAllPartnersByOptionTC1() {
    PartnerDto partner = setUpCorrectPartner(entityFactory);
    partnerRepository.create(partner);
    List<PartnerDto> partners = optionService.findAllPartnersByOption("BIN");
    assertEquals(1, partners.size());
  }

  @Test
  public void testFindAllPartnersByOptionTC2() {
    PartnerDto partner = setUpCorrectPartner(entityFactory);
    partner = partnerRepository.create(partner);
    List<PartnerDto> partners = optionService.findAllPartnersByOption("BIN");
    assertEquals(partner.getId(), partners.get(0).getId());
  }

  @Test
  public void testFindAllPartnersByOptionTC3() {
    PartnerDto partner1 = setUpCorrectPartner(entityFactory);
    partnerRepository.create(partner1);
    PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    option.setCode("BIM");
    option.setDepartement("BinDep");
    partnerOptionRepository.create(option, 1);

    PartnerDto partner2 = setUpCorrectPartner(entityFactory);
    partner2.setId(2);
    partnerRepository.create(partner2);
    assertEquals(2, optionService.findAllPartnersByOption("BIN").size());
    assertEquals(1, optionService.findAllPartnersByOption("BIM").size());
    assertEquals(0, optionService.findAllPartnersByOption("BDI").size());
  }

  /**
   * Creates an instance of PartnerDto with correct data.
   *
   * @return a partner ready to be inserted.
   */
  private PartnerDto setUpCorrectPartner(EntityFactory entityFactory) {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(1);
    partner.setBusinessName("Business partner");
    partner.setEmail("email@email.org");
    partner.setEmployeeCount(3);
    partner.setFullName("Full partner");
    partner.setLegalName("Legal partner");
    partner.setOrganisationType("Partner organization type");
    partner.setPhoneNumber("+32493184140");
    partner.setWebsite("www.partnerwebsite.com");
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(1);
    partner.setProgramme(programme);
    PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    option.setCode("BIN");
    option.setDepartement("BinDep");
    partnerOptionRepository.create(option, 1);
    List<PartnerOptionDto> options = new ArrayList<PartnerOptionDto>();
    options.add(option);
    partner.setOptions(options);
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setCity("Dublin");
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
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
