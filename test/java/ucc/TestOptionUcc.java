package java.ucc;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import java.util.ArrayList;
import java.util.List;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerOptionDao;
import com.dragomitch.ipl.pae.uccontrollers.OptionUcc;

public class TestOptionUcc {

  private EntityFactory entityFactory;
  private OptionUcc optionUcc;
  private PartnerDao partnerDao;
  private PartnerOptionDao partnerOptionDao;

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
    this.optionUcc = DependencyManager.getInstance(OptionUcc.class);
    this.partnerDao = DependencyManager.getInstance(PartnerDao.class);
    this.partnerOptionDao = DependencyManager.getInstance(PartnerOptionDao.class);
  }

  @After
  public void cleanUp() {
    ((MockPartnerDao) partnerDao).empty();
    ((MockPartnerOptionDao) partnerOptionDao).empty();
  }

  @Test
  public void testShowAllTC1() {
    assertEquals(5, optionUcc.showAll().size());
  }

  @Test
  public void testShowAllTC2() {
    List<OptionDto> options = optionUcc.showAll();
    assertEquals("BIN", options.get(0).getCode());
    assertEquals("BCH", options.get(1).getCode());
    assertEquals("BDI", options.get(2).getCode());
    assertEquals("BIM", options.get(3).getCode());
    assertEquals("BBM", options.get(4).getCode());
    assertEquals(5, optionUcc.showAll().size());
  }

  @Test
  public void testFindAllPartnersByOptionTC1() {
    PartnerDto partner = setUpCorrectPartner(entityFactory);
    partnerDao.create(partner);
    List<PartnerDto> partners = optionUcc.findAllPartnersByOption("BIN");
    assertEquals(1, partners.size());
  }

  @Test
  public void testFindAllPartnersByOptionTC2() {
    PartnerDto partner = setUpCorrectPartner(entityFactory);
    partner = partnerDao.create(partner);
    List<PartnerDto> partners = optionUcc.findAllPartnersByOption("BIN");
    assertEquals(partner.getId(), partners.get(0).getId());
  }

  @Test
  public void testFindAllPartnersByOptionTC3() {
    PartnerDto partner1 = setUpCorrectPartner(entityFactory);
    partnerDao.create(partner1);
    PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    option.setCode("BIM");
    option.setDepartement("BinDep");
    partnerOptionDao.create(option, 1);

    PartnerDto partner2 = setUpCorrectPartner(entityFactory);
    partner2.setId(2);
    partnerDao.create(partner2);
    assertEquals(2, optionUcc.findAllPartnersByOption("BIN").size());
    assertEquals(1, optionUcc.findAllPartnersByOption("BIM").size());
    assertEquals(0, optionUcc.findAllPartnersByOption("BDI").size());
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
    partnerOptionDao.create(option, 1);
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
