package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestNominatedStudent {

  @Autowired
  private ApplicationContext context;
  private static final int ID = 1;
  private static final int VERSION = 1;
  private static final String TITLE = "Mr";
  private static final LocalDate BIRTHDATE = LocalDate.now().minusDays(1);
  private static final String PHONE_NUMBER = "+32493";
  private static final String GENDER = "M";
  private static final int NBR_PASSED_YEARS = 2;
  private static final String IBAN = "BE92732143130176";
  private static final String CARD_HOLDER = "Card holder";
  private static final String BANK_NAME = "KBC";
  private static final String BIC = "KREDBEBB";
  private static final String COUNTRY_CODE = "BG";

  private static final String LONG_STRING_13 = "asdasdasdasda";
  private static final String LONG_STRING_36 = "asdasdasdasdasdasdasdasdasdasdasdasd";
  private static final String LONG_STRING_61 =
      "asdasdasdasdasdasdasdasdasdasdasasdasdasdasdasdasdasdasdasdas";

  private EntityFactory entityFactory;
  private NominatedStudent nominatedStudent;

  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
    nominatedStudent = setUpCorrectNominatedStudent();
  }

  @Test
  public void testGetAndSetIdTC1() {
    nominatedStudent.setId(ID);
    assertEquals(ID, nominatedStudent.getId());
  }

  @Test
  public void testGetAndSetTitleTC1() {
    nominatedStudent.setTitle(TITLE);
    assertEquals(TITLE, nominatedStudent.getTitle());
  }

  @Test
  public void testGetAndSetVersionTC1() {
    nominatedStudent.setVersion(VERSION);
    assertEquals(VERSION, nominatedStudent.getVersion());
  }

  @Test
  public void testGetAndSetBirthdateTC1() {
    nominatedStudent.setBirthdate(BIRTHDATE);
    assertEquals(BIRTHDATE, nominatedStudent.getBirthdate());
  }

  @Test
  public void testGetAndSetPhoneNumberTC1() {
    nominatedStudent.setPhoneNumber(PHONE_NUMBER);
    assertEquals(PHONE_NUMBER, nominatedStudent.getPhoneNumber());
  }

  @Test
  public void testGetAndSetGenderTC1() {
    nominatedStudent.setGender(GENDER);
    assertEquals(GENDER, nominatedStudent.getGender());
  }

  @Test
  public void testGetAndSetNbrPassedYearsTC1() {
    nominatedStudent.setNbrPassedYears(NBR_PASSED_YEARS);
    assertEquals(NBR_PASSED_YEARS, nominatedStudent.getNbrPassedYears());
  }

  @Test
  public void testGetAndSetIbanTC1() {
    nominatedStudent.setIban(IBAN);
    assertEquals(IBAN, nominatedStudent.getIban());
  }

  @Test
  public void testGetAndSetCardHolderTC1() {
    nominatedStudent.setCardHolder(CARD_HOLDER);
    assertEquals(CARD_HOLDER, nominatedStudent.getCardHolder());
  }

  @Test
  public void testGetAndSetBankNameTC1() {
    nominatedStudent.setBankName(BANK_NAME);
    assertEquals(BANK_NAME, nominatedStudent.getBankName());
  }

  @Test
  public void testGetAndSetBicTC1() {
    nominatedStudent.setBic(BIC);
    assertEquals(BIC, nominatedStudent.getBic());
  }

  @Test
  public void testGetAndSetVersionTC2() {
    nominatedStudent.setVersion(VERSION);
    assertEquals(VERSION, nominatedStudent.getVersion());
  }

  @Test
  public void testCheckDataIntegrityTC1() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setTitle(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC2() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setTitle("");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC3() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setTitle("Ntfs");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC4() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setTitle("Msr");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC5() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBirthdate(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC6() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBirthdate(LocalDate.now().plusDays(1));
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC7() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setNationality(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC8() {
    assertThrows(BusinessException.class, () -> {
      CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
      nominatedStudent.setNationality(country);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC9() {
    assertThrows(BusinessException.class, () -> {
      CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
      country.setCountryCode("1");
      nominatedStudent.setNationality(country);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC10() {
    assertThrows(BusinessException.class, () -> {
      CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
      country.setCountryCode("123");
      nominatedStudent.setNationality(country);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC11() {
    assertThrows(BusinessException.class, () -> {
      CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
      country.setCountryCode("1");
      nominatedStudent.setNationality(country);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC12() {
    assertThrows(BusinessException.class, () -> {
      CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
      country.setCountryCode("123");
      nominatedStudent.setNationality(country);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC13() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setAddress(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC14() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setGender(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC15() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setGender("");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC16() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setGender("D");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC17() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setNbrPassedYears(-1);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC18() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setNbrPassedYears(0);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC19() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setGender("D");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC20() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setIban(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC21() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setIban("");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC22() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setIban("B92732143130176");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC23() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setIban("88927321431301768");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC24() {
    nominatedStudent.setCardHolder(null);
    nominatedStudent.checkDataIntegrity();
    assertEquals(nominatedStudent.getFirstName() + " " + nominatedStudent.getLastName(),
        nominatedStudent.getCardHolder());
  }

  @Test
  public void testCheckDataIntegrityTC25() {
    nominatedStudent.setCardHolder("");
    nominatedStudent.checkDataIntegrity();
    assertEquals(nominatedStudent.getFirstName() + " " + nominatedStudent.getLastName(),
        nominatedStudent.getCardHolder());
  }

  @Test
  public void testCheckDataIntegrityTC26() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setCardHolder(LONG_STRING_36);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC27() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBankName(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC28() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBankName("");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC29() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBankName(LONG_STRING_61);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC30() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic(null);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC31() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic("");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC32() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic(LONG_STRING_13);
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC33() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic("KRIBEBEBB");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC34() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic("KREDBEB");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC35() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic("KRIBEBEBBKRE");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC36() {
    assertThrows(BusinessException.class, () -> {
      nominatedStudent.setBic("KREDB8BB");
      nominatedStudent.checkDataIntegrity();
    });
  }

  @Test
  public void testCheckDataIntegrityTC37() {
    nominatedStudent.checkDataIntegrity();
  }

  private NominatedStudent setUpCorrectNominatedStudent() {
    NominatedStudent nominatedStudent =
        (NominatedStudent) entityFactory.build(NominatedStudent.class);
    nominatedStudent.setLastName(TestUser.LAST_NAME);
    nominatedStudent.setTitle(TITLE);
    nominatedStudent.setBirthdate(BIRTHDATE);
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(COUNTRY_CODE);
    nominatedStudent.setNationality(country);
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    nominatedStudent.setAddress(address);
    nominatedStudent.setPhoneNumber(PHONE_NUMBER);
    nominatedStudent.setGender(GENDER);
    nominatedStudent.setNbrPassedYears(NBR_PASSED_YEARS);
    nominatedStudent.setIban(IBAN);
    nominatedStudent.setCardHolder(CARD_HOLDER);
    nominatedStudent.setBankName(BANK_NAME);
    nominatedStudent.setBic(BIC);
    return nominatedStudent;
  }
}
