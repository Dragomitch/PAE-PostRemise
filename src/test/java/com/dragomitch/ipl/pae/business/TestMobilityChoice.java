package java.business;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.MobilityChoice;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;

public class TestMobilityChoice {
  private EntityFactory entityFactory;
  private MobilityChoice correctMobilityChoice;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Set up an object for tests.
   * 
   * @throws Exception If an orrur is occured, we trow it upper.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    correctMobilityChoice = (MobilityChoice) entityFactory.build(MobilityChoice.class);
    correctMobilityChoice.setId(2);
    correctMobilityChoice.setPreferenceOrder(2);
    correctMobilityChoice.setMobilityType(MobilityChoice.MOBILITY_TYPE_SMS);
    correctMobilityChoice.setAcademicYear(2);
    correctMobilityChoice.setTerm(MobilityChoice.MAX_TERM_CHOICE);
    correctMobilityChoice.setVersion(2);
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    correctMobilityChoice.setUser(user);
    correctMobilityChoice.getUser().setId(1);
    correctMobilityChoice.setProgramme((ProgrammeDto) entityFactory.build(ProgrammeDto.class));
    correctMobilityChoice.getProgramme().setId(1);
    correctMobilityChoice.setCountry((CountryDto) entityFactory.build(CountryDto.class));
    correctMobilityChoice.getCountry().setCountryCode("BE");
  }

  @Test
  public void testSetAndGetIdTC1() {
    int value = 22;
    correctMobilityChoice.setId(value);
    assertEquals("The id is not properly setted or getted", value, correctMobilityChoice.getId());
  }

  @Test
  public void testSetAndGetIdTC2() {
    int firstValue = 22;
    int secondValue = 55;
    correctMobilityChoice.setId(firstValue);
    assertEquals("The id is not properly setted or getted", firstValue,
        correctMobilityChoice.getId());
    correctMobilityChoice.setId(secondValue);
    assertEquals("The second id is not properly setted or getted", secondValue,
        correctMobilityChoice.getId());
  }

  @Test
  public void testSetAndGetUserTC1() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    correctMobilityChoice.setUser(user);
    assertEquals("The User is not the one expected", user, correctMobilityChoice.getUser());
  }

  @Test
  public void testSetAndGetUserTC2() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    UserDto user2 = (UserDto) entityFactory.build(UserDto.class);
    correctMobilityChoice.setUser(user);
    assertEquals("The User is not the one expected", user, correctMobilityChoice.getUser());
    correctMobilityChoice.setUser(user2);
    assertEquals("The User2 is not the one expected", user2, correctMobilityChoice.getUser());
  }

  @Test
  public void testSetAndGetPreferenceOrderTC1() {
    int value = 4;
    correctMobilityChoice.setPreferenceOrder(value);
    assertEquals("The preference order is not the one expected", value,
        correctMobilityChoice.getPreferenceOrder());
  }

  @Test
  public void testSetAndGetPreferenceOrderTC2() {
    int value = 4;
    int value2 = 6;
    correctMobilityChoice.setPreferenceOrder(value);
    assertEquals("The preference order is not the one expected", value,
        correctMobilityChoice.getPreferenceOrder());
    correctMobilityChoice.setPreferenceOrder(value2);
    assertEquals("The preference order no2 is not the one expected", value2,
        correctMobilityChoice.getPreferenceOrder());
  }

  @Test
  public void testSetAndGetMobilityTypeTC1() {
    String value = "test";
    correctMobilityChoice.setMobilityType(value);
    assertEquals("The MobilityType is not the one expected", value,
        correctMobilityChoice.getMobilityType());
  }

  @Test
  public void testSetAndGetMobilityTypeTC2() {
    String value = "test";
    String value2 = "test2";
    correctMobilityChoice.setMobilityType(value);
    assertEquals("The mobilityType is not the one expected", value,
        correctMobilityChoice.getMobilityType());
    correctMobilityChoice.setMobilityType(value2);
    assertEquals("The mobilityType no2 is not the one expected", value2,
        correctMobilityChoice.getMobilityType());
  }

  @Test
  public void testSetAndGetAcademicYearTC1() {
    int value = 2016;
    correctMobilityChoice.setAcademicYear(value);
    assertEquals("The academicYear value is no the one expected", value,
        correctMobilityChoice.getAcademicYear());
  }

  @Test
  public void testSetAndGetAcademicYearTC2() {
    int value = 2016;
    int value2 = 2017;
    correctMobilityChoice.setAcademicYear(value);
    assertEquals("The academicYear value is not the one expected", value,
        correctMobilityChoice.getAcademicYear());
    correctMobilityChoice.setAcademicYear(value2);
    assertEquals("The academicYear value no2 is no the one expected", value2,
        correctMobilityChoice.getAcademicYear());
  }

  @Test
  public void testSetAndGetTermTC1() {
    int value = 1;
    correctMobilityChoice.setTerm(value);
    assertEquals("The term value is not the one expected", value, correctMobilityChoice.getTerm());
  }

  @Test
  public void testSetAndGetTermTC2() {
    int value = 1;
    int value2 = 3;
    correctMobilityChoice.setTerm(value);
    assertEquals("The term value is not the one expected", value, correctMobilityChoice.getTerm());
    correctMobilityChoice.setTerm(value2);
    assertEquals("The term value no2 is not the one expected", value2,
        correctMobilityChoice.getTerm());
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    correctMobilityChoice.setProgramme(programme);
    assertEquals("The programme is not the one expected", programme,
        correctMobilityChoice.getProgramme());
  }

  @Test
  public void testSetAndGetProgrammeTC2() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    ProgrammeDto programme2 = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    correctMobilityChoice.setProgramme(programme);
    assertEquals("The programme is not the one expected", programme,
        correctMobilityChoice.getProgramme());
    correctMobilityChoice.setProgramme(programme2);
    assertEquals("The programme no2 is not the one expected", programme2,
        correctMobilityChoice.getProgramme());
  }

  @Test
  public void testSetAndGetCountryTC1() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    correctMobilityChoice.setCountry(country);
    assertEquals("The country is not the one expected", country,
        correctMobilityChoice.getCountry());
  }

  @Test
  public void testSetAndGetCountryTC2() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    CountryDto country2 = (CountryDto) entityFactory.build(CountryDto.class);
    correctMobilityChoice.setCountry(country);
    assertEquals("The country is not the one expected", country,
        correctMobilityChoice.getCountry());
    correctMobilityChoice.setCountry(country2);
    assertEquals("The country no2 is not the one expected", country2,
        correctMobilityChoice.getCountry());
  }

  @Test
  public void testSetAndGetSubmisssionDateTC1() {
    LocalDateTime time = LocalDateTime.now();
    correctMobilityChoice.setSubmissionDate(time);
    assertEquals("The submissionDate is not the one expected", time,
        correctMobilityChoice.getSubmissionDate());
  }

  @Test
  public void testSetAndGetSubmisssionDateTC2() {
    LocalDateTime time = LocalDateTime.now();
    LocalDateTime time2 = LocalDateTime.now();
    correctMobilityChoice.setSubmissionDate(time);
    assertEquals("The submissionDate is not the one expected", time,
        correctMobilityChoice.getSubmissionDate());
    correctMobilityChoice.setSubmissionDate(time2);
    assertEquals("The submissionDate no2 is not the one expected", time2,
        correctMobilityChoice.getSubmissionDate());
  }

  @Test
  public void testSetAndGetDenialReasonTC1() {
    DenialReasonDto dr = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    correctMobilityChoice.setDenialReason(dr);
    assertEquals("The denialReason is not the one expected", dr,
        correctMobilityChoice.getDenialReason());
  }

  @Test
  public void testSetAndGetDenialReasonTC2() {
    DenialReasonDto dr = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    DenialReasonDto dr2 = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    correctMobilityChoice.setDenialReason(dr);
    assertEquals("The denialReason is not the one expected", dr,
        correctMobilityChoice.getDenialReason());
    correctMobilityChoice.setDenialReason(dr2);
    assertEquals("The denialReason no2 is not the one expected", dr2,
        correctMobilityChoice.getDenialReason());
  }

  @Test
  public void testSetAndGetCancellationReasonTC1() {
    String cancel = "Veut plus partir :(";
    correctMobilityChoice.setCancellationReason(cancel);
    assertEquals("The cancellationReason is not the one expected", cancel,
        correctMobilityChoice.getCancellationReason());
  }

  @Test
  public void testSetAndGetCancellationReasonTC2() {
    String cancel = "Veut plus partir :(";
    String cancel2 = "Veut plus partir";
    correctMobilityChoice.setCancellationReason(cancel);
    assertEquals("The cancellationReason is not the one expected", cancel,
        correctMobilityChoice.getCancellationReason());
    correctMobilityChoice.setCancellationReason(cancel2);
    assertEquals("The cancellationReason no2 is not the one expected", cancel2,
        correctMobilityChoice.getCancellationReason());
  }

  @Test
  public void testSetANdGetPartnerTC1() {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    correctMobilityChoice.setPartner(partner);
    assertEquals("The partner value is not the one expected", partner,
        correctMobilityChoice.getPartner());
  }

  @Test
  public void testSetANdGetPartnerTC2() {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    PartnerDto partner2 = (PartnerDto) entityFactory.build(PartnerDto.class);
    correctMobilityChoice.setPartner(partner);
    assertEquals("The partner value is not the one expected", partner,
        correctMobilityChoice.getPartner());
    correctMobilityChoice.setPartner(partner2);
    assertEquals("The partner no2 value is not the one expected", partner2,
        correctMobilityChoice.getPartner());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    int value = 3;
    correctMobilityChoice.setVersion(value);
    assertEquals("The version value is not the one expected", value,
        correctMobilityChoice.getVersion());
  }

  @Test
  public void testSetAndGetVersionTC2() {
    int value = 3;
    int value2 = 5;
    correctMobilityChoice.setVersion(value);
    assertEquals("The version value is not the one expected", value,
        correctMobilityChoice.getVersion());
    correctMobilityChoice.setVersion(value2);
    assertEquals("The version no2 value is not the one expected", value2,
        correctMobilityChoice.getVersion());
  }

  @Test
  public void testCheckDataIntegrityTC0() {
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC1() {
    int prefenceOrder = 0;
    correctMobilityChoice.setPreferenceOrder(prefenceOrder);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC2() {
    int prefenceOrder = 4;
    correctMobilityChoice.setPreferenceOrder(prefenceOrder);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC4() {
    String mobilityType = "";
    correctMobilityChoice.setMobilityType(mobilityType);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC5() {
    String mobilityType = "Test";
    correctMobilityChoice.setMobilityType(mobilityType);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test
  public void testCheckDataIntegrityTC6() {
    String mobilityType = "SMS";
    correctMobilityChoice.setMobilityType(mobilityType);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test
  public void testCheckDataIntegrityTC7() {
    String mobilityType = "SMP";
    correctMobilityChoice.setMobilityType(mobilityType);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC8() {
    int academicYear = -1;
    correctMobilityChoice.setAcademicYear(academicYear);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC9() {
    int academicYear = 0;
    correctMobilityChoice.setAcademicYear(academicYear);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC10() {
    int term = -1;
    correctMobilityChoice.setTerm(term);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC11() {
    int term = 0;
    correctMobilityChoice.setTerm(term);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC12() {
    int term = 3;
    correctMobilityChoice.setTerm(term);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC13() {
    correctMobilityChoice.setUser(null);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC14() {
    correctMobilityChoice.getUser().setId(0);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC17() {
    correctMobilityChoice.setProgramme(null);
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC18() {
    correctMobilityChoice.getProgramme().setId(0);;
    correctMobilityChoice.checkDataIntegrity();
  }
}
