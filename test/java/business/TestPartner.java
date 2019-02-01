package java.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Partner;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestPartner {

  private static final int ID = 1;
  private static final String LEGALNAME = "SpeculoosFactory";
  private static final String BUSINESSNAME = "Speculation & co";
  private static final String FULLNAME = "Speculation with Speculoos company";
  private static final String ORGANISATIONTYPE = "PME";
  private static final int EMPLOYEECOUNT = 25;
  private static final String PHONENUMBER = "+322/478/19/15";
  private static final String EMAIL = "Speculoos@specu.co";
  private static final String WEBSITE = "www.SpeculoosAndSpeculation.com";
  private static final boolean STATUS = true;
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private Partner partner;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Partner instance and populates its "address","options" and "programme"
   * attributes.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    partner = (Partner) entityFactory.build(Partner.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    partner.setId(ID);
    assertEquals(ID, partner.getId());
  }

  @Test
  public void testSetAndGetLegalNameTC1() {
    partner.setLegalName(LEGALNAME);
    assertEquals(LEGALNAME, partner.getLegalName());
  }

  @Test
  public void testSetAndGetBusinessNameTC1() {
    partner.setBusinessName(BUSINESSNAME);
    assertEquals(BUSINESSNAME, partner.getBusinessName());
  }

  @Test
  public void testSetAndGetFullNameTC1() {
    partner.setFullName(FULLNAME);
    assertEquals(FULLNAME, partner.getFullName());
  }

  @Test
  public void testSetAndGetOrganisationTypeTC1() {
    partner.setOrganisationType(ORGANISATIONTYPE);
    assertEquals(ORGANISATIONTYPE, partner.getOrganisationType());
  }

  @Test
  public void testSetAndGetEmployeeCountTC1() {
    partner.setEmployeeCount(EMPLOYEECOUNT);
    assertEquals(EMPLOYEECOUNT, partner.getEmployeeCount());
  }

  @Test
  public void testSetAndGetPhoneNumberTC1() {
    partner.setPhoneNumber(PHONENUMBER);
    assertEquals(PHONENUMBER, partner.getPhoneNumber());
  }

  @Test
  public void testSetAndGetEmailTC1() {
    partner.setEmail(EMAIL);
    assertEquals(EMAIL, partner.getEmail());
  }

  @Test
  public void testSetAndGetWebsiteTC1() {
    partner.setWebsite(WEBSITE);
    assertEquals(WEBSITE, partner.getWebsite());
  }

  @Test
  public void testSetAndGetStatusTC1() {
    partner.setStatus(STATUS);
    assertTrue(partner.isOfficial());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    partner.setVersion(VERSION);
    assertEquals(VERSION, partner.getVersion());
  }

  @Test
  public void testSetAndGetAddressTC1() {
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    partner.setAddress(address);
    assertEquals(address, partner.getAddress());
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    partner.setProgramme(programme);
    assertEquals(programme, partner.getProgramme());
  }

  @Test
  public void testSetAndGetOptionsTC1() {
    List<PartnerOptionDto> options = new ArrayList<PartnerOptionDto>();
    PartnerOptionDto option1 = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    PartnerOptionDto option2 = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    options.add(option1);
    options.add(option2);
    partner.setOptions(options);
    assertEquals(options, partner.getOptions());
  }
}
