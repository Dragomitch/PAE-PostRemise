package com.dragomitch.ipl.pae.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.UnitTestConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestPartner {

  @Autowired private ApplicationContext context;

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

  private Partner partner;

  /**
   * Creates a new Partner instance and populates its "address","options" and "programme"
   * attributes.
   */
  @BeforeEach
  public void setUp() throws Exception {
    partner = new Partner();
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
    partner.setOfficial(STATUS);
    assertTrue(partner.isOfficial());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    partner.setVersion(VERSION);
    assertEquals(VERSION, partner.getVersion());
  }

  @Test
  public void testSetAndGetAddressTC1() {
    Address address = new Address();
    partner.setAddress(address);
    assertEquals(address, partner.getAddress());
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    Programme programme = new Programme();
    partner.setProgramme(programme);
    assertEquals(programme, partner.getProgramme());
  }

  @Test
  public void testSetAndGetOptionsTC1() {
    List<PartnerOption> options = new ArrayList<PartnerOption>();
    PartnerOption option1 = new PartnerOption();
    PartnerOption option2 = new PartnerOption();
    options.add(option1);
    options.add(option2);
    partner.setOptions(options);
    assertEquals(options, partner.getOptions());
  }
}
