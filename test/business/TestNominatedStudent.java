package business;

import static org.junit.Assert.assertEquals;

import business.dto.AddressDto;
import business.dto.CountryDto;
import business.exceptions.BusinessException;
import main.ContextManager;
import main.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;

public class TestNominatedStudent {
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

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
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

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC1() {
    nominatedStudent.setTitle(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC2() {
    nominatedStudent.setTitle("");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC3() {
    nominatedStudent.setTitle("Ntfs");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC4() {
    nominatedStudent.setTitle("Msr");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC5() {
    nominatedStudent.setBirthdate(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC6() {
    nominatedStudent.setBirthdate(LocalDate.now().plusDays(1));
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC7() {
    nominatedStudent.setNationality(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC8() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    nominatedStudent.setNationality(country);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC9() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("1");
    nominatedStudent.setNationality(country);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC10() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("123");
    nominatedStudent.setNationality(country);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC11() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("1");
    nominatedStudent.setNationality(country);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC12() {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("123");
    nominatedStudent.setNationality(country);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC13() {
    nominatedStudent.setAddress(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC14() {
    nominatedStudent.setGender(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC15() {
    nominatedStudent.setGender("");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC16() {
    nominatedStudent.setGender("D");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC17() {
    nominatedStudent.setNbrPassedYears(-1);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC18() {
    nominatedStudent.setNbrPassedYears(0);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC19() {
    nominatedStudent.setGender("D");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC20() {
    nominatedStudent.setIban(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC21() {
    nominatedStudent.setIban("");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC22() {
    nominatedStudent.setIban("B92732143130176");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC23() {
    nominatedStudent.setIban("88927321431301768");
    nominatedStudent.checkDataIntegrity();
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

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC26() {
    nominatedStudent.setCardHolder(LONG_STRING_36);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC27() {
    nominatedStudent.setBankName(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC28() {
    nominatedStudent.setBankName("");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC29() {
    nominatedStudent.setBankName(LONG_STRING_61);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC30() {
    nominatedStudent.setBic(null);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC31() {
    nominatedStudent.setBic("");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC32() {
    nominatedStudent.setBic(LONG_STRING_13);
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC33() {
    nominatedStudent.setBic("KRIBEBEBB");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC34() {
    nominatedStudent.setBic("KREDBEB");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC35() {
    nominatedStudent.setBic("KRIBEBEBBKRE");
    nominatedStudent.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC36() {
    nominatedStudent.setBic("KREDB8BB");
    nominatedStudent.checkDataIntegrity();
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
