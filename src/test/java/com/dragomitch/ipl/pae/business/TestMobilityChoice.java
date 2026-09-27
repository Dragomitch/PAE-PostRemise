package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.MobilityChoice;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestMobilityChoice {

  @Autowired
  private ApplicationContext context;
  private EntityFactory entityFactory;
  private MobilityChoice correctMobilityChoice;

  /**
   * Set up an object for tests.
   * 
   * @throws Exception If an orrur is occured, we trow it upper.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
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
    assertEquals(value, correctMobilityChoice.getId(), "The id is not properly setted or getted");
  }

  @Test
  public void testSetAndGetIdTC2() {
    int firstValue = 22;
    int secondValue = 55;
    correctMobilityChoice.setId(firstValue);
    assertEquals(firstValue, correctMobilityChoice.getId(), "The id is not properly setted or getted");
    correctMobilityChoice.setId(secondValue);
    assertEquals(secondValue, correctMobilityChoice.getId(), "The second id is not properly setted or getted");
  }

  @Test
  public void testSetAndGetUserTC1() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    correctMobilityChoice.setUser(user);
    assertEquals(user, correctMobilityChoice.getUser(), "The User is not the one expected");
  }

  @Test
  public void testSetAndGetUserTC2() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    UserDto user2 = (UserDto) entityFactory.build(UserDto.class);
    correctMobilityChoice.setUser(user);
    assertEquals(user, correctMobilityChoice.getUser(), "The User is not the one expected");
    correctMobilityChoice.setUser(user2);
    assertEquals(user2, correctMobilityChoice.getUser(), "The User2 is not the one expected");
  }

  @Test
  public void testSetAndGetPreferenceOrderTC1() {
    int value = 4;
    correctMobilityChoice.setPreferenceOrder(value);
    assertEquals(value, correctMobilityChoice.getPreferenceOrder(), "The preference order is not the one expected");
  }

  @Test
  public void testSetAndGetPreferenceOrderTC2() {
    int value = 4;
    int value2 = 6;
    correctMobilityChoice.setPreferenceOrder(value);
    assertEquals(value, correctMobilityChoice.getPreferenceOrder(), "The preference order is not the one expected");
    correctMobilityChoice.setPreferenceOrder(value2);
    assertEquals(value2, correctMobilityChoice.getPreferenceOrder(), "The preference order no2 is not the one expected");
  }

  @Test
  public void testSetAndGetMobilityTypeTC1() {
    String value = "test";
    correctMobilityChoice.setMobilityType(value);
    assertEquals(value, correctMobilityChoice.getMobilityType(), "The MobilityType is not the one expected");
  }

  @Test
  public void testSetAndGetMobilityTypeTC2() {
    String value = "test";
    String value2 = "test2";
    correctMobilityChoice.setMobilityType(value);
    assertEquals(value, correctMobilityChoice.getMobilityType(), "The mobilityType is not the one expected");
    correctMobilityChoice.setMobilityType(value2);
    assertEquals(value2, correctMobilityChoice.getMobilityType(), "The mobilityType no2 is not the one expected");
  }

  @Test
  public void testSetAndGetAcademicYearTC1() {
    int value = 2016;
    correctMobilityChoice.setAcademicYear(value);
    assertEquals(value, correctMobilityChoice.getAcademicYear(), "The academicYear value is no the one expected");
  }

  @Test
  public void testSetAndGetAcademicYearTC2() {
    int value = 2016;
    int value2 = 2017;
    correctMobilityChoice.setAcademicYear(value);
    assertEquals(value, correctMobilityChoice.getAcademicYear(), "The academicYear value is not the one expected");
    correctMobilityChoice.setAcademicYear(value2);
    assertEquals(value2, correctMobilityChoice.getAcademicYear(), "The academicYear value no2 is no the one expected");
  }

  @Test
  public void testSetAndGetTermTC1() {
    int value = 1;
    correctMobilityChoice.setTerm(value);
    assertEquals(value, correctMobilityChoice.getTerm(), "The term value is not the one expected");
  }

  @Test
  public void testSetAndGetTermTC2() {
    int value = 1;
    int value2 = 3;
    correctMobilityChoice.setTerm(value);
    assertEquals(value, correctMobilityChoice.getTerm(), "The term value is not the one expected");
    correctMobilityChoice.setTerm(value2);
    assertEquals(value2, correctMobilityChoice.getTerm(), "The term value no2 is not the one expected");
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    correctMobilityChoice.setProgramme(programme);
    assertEquals(programme, correctMobilityChoice.getProgramme(), "The programme is not the one expected");
  }

  @Test
  public void testSetAndGetProgrammeTC2() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    ProgrammeDto programme2 = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    correctMobilityChoice.setProgramme(programme);
    assertEquals(programme, correctMobilityChoice.getProgramme(), "The programme is not the one expected");
    correctMobilityChoice.setProgramme(programme2);
    assertEquals(programme2, correctMobilityChoice.getProgramme(), "The programme no2 is not the one expected");
  }

  @Test
  public void testSetAndGetCountryTC1() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    correctMobilityChoice.setCountry(country);
    assertEquals(country, correctMobilityChoice.getCountry(), "The country is not the one expected");
  }

  @Test
  public void testSetAndGetCountryTC2() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    CountryDto country2 = (CountryDto) entityFactory.build(CountryDto.class);
    correctMobilityChoice.setCountry(country);
    assertEquals(country, correctMobilityChoice.getCountry(), "The country is not the one expected");
    correctMobilityChoice.setCountry(country2);
    assertEquals(country2, correctMobilityChoice.getCountry(), "The country no2 is not the one expected");
  }

  @Test
  public void testSetAndGetSubmisssionDateTC1() {
    LocalDateTime time = LocalDateTime.now();
    correctMobilityChoice.setSubmissionDate(time);
    assertEquals(time, correctMobilityChoice.getSubmissionDate(), "The submissionDate is not the one expected");
  }

  @Test
  public void testSetAndGetSubmisssionDateTC2() {
    LocalDateTime time = LocalDateTime.now();
    LocalDateTime time2 = LocalDateTime.now();
    correctMobilityChoice.setSubmissionDate(time);
    assertEquals(time, correctMobilityChoice.getSubmissionDate(), "The submissionDate is not the one expected");
    correctMobilityChoice.setSubmissionDate(time2);
    assertEquals(time2, correctMobilityChoice.getSubmissionDate(), "The submissionDate no2 is not the one expected");
  }

  @Test
  public void testSetAndGetDenialReasonTC1() {
    DenialReasonDto dr = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    correctMobilityChoice.setDenialReason(dr);
    assertEquals(dr, correctMobilityChoice.getDenialReason(), "The denialReason is not the one expected");
  }

  @Test
  public void testSetAndGetDenialReasonTC2() {
    DenialReasonDto dr = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    DenialReasonDto dr2 = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    correctMobilityChoice.setDenialReason(dr);
    assertEquals(dr, correctMobilityChoice.getDenialReason(), "The denialReason is not the one expected");
    correctMobilityChoice.setDenialReason(dr2);
    assertEquals(dr2, correctMobilityChoice.getDenialReason(), "The denialReason no2 is not the one expected");
  }

  @Test
  public void testSetAndGetCancellationReasonTC1() {
    String cancel = "Veut plus partir :(";
    correctMobilityChoice.setCancellationReason(cancel);
    assertEquals(cancel, correctMobilityChoice.getCancellationReason(), "The cancellationReason is not the one expected");
  }

  @Test
  public void testSetAndGetCancellationReasonTC2() {
    String cancel = "Veut plus partir :(";
    String cancel2 = "Veut plus partir";
    correctMobilityChoice.setCancellationReason(cancel);
    assertEquals(cancel, correctMobilityChoice.getCancellationReason(), "The cancellationReason is not the one expected");
    correctMobilityChoice.setCancellationReason(cancel2);
    assertEquals(cancel2, correctMobilityChoice.getCancellationReason(), "The cancellationReason no2 is not the one expected");
  }

  @Test
  public void testSetANdGetPartnerTC1() {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    correctMobilityChoice.setPartner(partner);
    assertEquals(partner, correctMobilityChoice.getPartner(), "The partner value is not the one expected");
  }

  @Test
  public void testSetANdGetPartnerTC2() {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    PartnerDto partner2 = (PartnerDto) entityFactory.build(PartnerDto.class);
    correctMobilityChoice.setPartner(partner);
    assertEquals(partner, correctMobilityChoice.getPartner(), "The partner value is not the one expected");
    correctMobilityChoice.setPartner(partner2);
    assertEquals(partner2, correctMobilityChoice.getPartner(), "The partner no2 value is not the one expected");
  }

  @Test
  public void testSetAndGetVersionTC1() {
    int value = 3;
    correctMobilityChoice.setVersion(value);
    assertEquals(value, correctMobilityChoice.getVersion(), "The version value is not the one expected");
  }

  @Test
  public void testSetAndGetVersionTC2() {
    int value = 3;
    int value2 = 5;
    correctMobilityChoice.setVersion(value);
    assertEquals(value, correctMobilityChoice.getVersion(), "The version value is not the one expected");
    correctMobilityChoice.setVersion(value2);
    assertEquals(value2, correctMobilityChoice.getVersion(), "The version no2 value is not the one expected");
  }

  @Test
  public void testCheckDataIntegrityTC0() {
    correctMobilityChoice.checkDataIntegrity();
  }

  @Test
  public void testCheckDataIntegrityTC1() {
    assertThrows(BusinessException.class, () -> {
      int prefenceOrder = 0;
      correctMobilityChoice.setPreferenceOrder(prefenceOrder);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC2() {
    assertThrows(BusinessException.class, () -> {
      int prefenceOrder = 4;
      correctMobilityChoice.setPreferenceOrder(prefenceOrder);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC4() {
    assertThrows(BusinessException.class, () -> {
      String mobilityType = "";
      correctMobilityChoice.setMobilityType(mobilityType);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC5() {
    assertThrows(BusinessException.class, () -> {
      String mobilityType = "Test";
      correctMobilityChoice.setMobilityType(mobilityType);
      correctMobilityChoice.checkDataIntegrity();
    });
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

  @Test
  public void testCheckDataIntegrityTC8() {
    assertThrows(BusinessException.class, () -> {
      int academicYear = -1;
      correctMobilityChoice.setAcademicYear(academicYear);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC9() {
    assertThrows(BusinessException.class, () -> {
      int academicYear = 0;
      correctMobilityChoice.setAcademicYear(academicYear);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC10() {
    assertThrows(BusinessException.class, () -> {
      int term = -1;
      correctMobilityChoice.setTerm(term);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC11() {
    assertThrows(BusinessException.class, () -> {
      int term = 0;
      correctMobilityChoice.setTerm(term);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC12() {
    assertThrows(BusinessException.class, () -> {
      int term = 3;
      correctMobilityChoice.setTerm(term);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC13() {
    assertThrows(BusinessException.class, () -> {
      correctMobilityChoice.setUser(null);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC14() {
    assertThrows(BusinessException.class, () -> {
      correctMobilityChoice.getUser().setId(0);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC17() {
    assertThrows(BusinessException.class, () -> {
      correctMobilityChoice.setProgramme(null);
      correctMobilityChoice.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC18() {
    assertThrows(BusinessException.class, () -> {
      correctMobilityChoice.getProgramme().setId(0);;
      correctMobilityChoice.checkDataIntegrity();
    });
  }
}
