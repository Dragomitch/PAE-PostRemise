package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  private static final String IBAN = "BE68539007547034";
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
    nominatedStudent.setTitle(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC2() {
    nominatedStudent.setTitle("");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC3() {
    nominatedStudent.setTitle("Ntfs");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC4() {
    nominatedStudent.setTitle("Msr");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC5() {
    nominatedStudent.setBirthdate(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC6() {
    nominatedStudent.setBirthdate(LocalDate.now().plusDays(1));
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC7() {
    nominatedStudent.setNationality(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC8() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    nominatedStudent.setNationality(country);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC9() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("1");
    nominatedStudent.setNationality(country);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC10() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("123");
    nominatedStudent.setNationality(country);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC11() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("1");
    nominatedStudent.setNationality(country);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC12() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("123");
    nominatedStudent.setNationality(country);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC13() {
    nominatedStudent.setAddress(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC14() {
    nominatedStudent.setGender(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC15() {
    nominatedStudent.setGender("");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC16() {
    nominatedStudent.setGender("D");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC17() {
    nominatedStudent.setNbrPassedYears(-1);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC18() {
    nominatedStudent.setNbrPassedYears(0);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC19() {
    nominatedStudent.setGender("D");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC20() {
    nominatedStudent.setIban(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC21() {
    nominatedStudent.setIban("");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC22() {
    nominatedStudent.setIban("B92732143130176");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC23() {
    nominatedStudent.setIban("88927321431301768");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC24() {
    // the card holder is optional: NominatedStudentUcc then uses the name of the student
    nominatedStudent.setCardHolder(null);
    assertEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC25() {
    nominatedStudent.setCardHolder("");
    assertEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC26() {
    nominatedStudent.setCardHolder(LONG_STRING_36);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC27() {
    nominatedStudent.setBankName(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC28() {
    nominatedStudent.setBankName("");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC29() {
    nominatedStudent.setBankName(LONG_STRING_61);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC30() {
    nominatedStudent.setBic(null);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC31() {
    nominatedStudent.setBic("");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC32() {
    nominatedStudent.setBic(LONG_STRING_13);
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC33() {
    nominatedStudent.setBic("KRIBEBEBB");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC34() {
    nominatedStudent.setBic("KREDBEB");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC35() {
    nominatedStudent.setBic("KRIBEBEBBKRE");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC36() {
    nominatedStudent.setBic("KREDB8BB");
    assertNotEquals(List.of(), Violations.of(nominatedStudent));
  }

  @Test
  public void testCheckDataIntegrityTC37() {
    assertEquals(List.of(), Violations.of(nominatedStudent));
  }

  private NominatedStudent setUpCorrectNominatedStudent() {
    NominatedStudent nominatedStudent =
        (NominatedStudent) entityFactory.build(NominatedStudent.class);
    nominatedStudent.setLastName(TestUser.LAST_NAME);
    nominatedStudent.setFirstName("First Name");
    nominatedStudent.setUsername("username");
    nominatedStudent.setEmail("email@do.com");
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode("BIN");
    nominatedStudent.setOption(option);
    nominatedStudent.setTitle(TITLE);
    nominatedStudent.setBirthdate(BIRTHDATE);
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(COUNTRY_CODE);
    nominatedStudent.setNationality(country);
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setStreet("Rue de la Loi");
    address.setNumber("16");
    address.setCity("Bruxelles");
    address.setPostalCode("1000");
    address.setRegion("");
    CountryDto addressCountry = (CountryDto) entityFactory.build(CountryDto.class);
    addressCountry.setCountryCode("BE");
    address.setCountry(addressCountry);
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
