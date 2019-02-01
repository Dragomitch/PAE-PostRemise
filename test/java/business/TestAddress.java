package java.business;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAddress {

  private static final int ID = 1;
  private static final String STREET = "Rue de l'aurée du bois";
  private static final String NUMBER = "69-70";
  private static final String CITY = "Bruxelles";
  private static final String POSTALCODE = "1090";
  private static final String REGION = "Bruxelles capitale";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private Address address;
  private CountryDto countryDto;
  private static final String COUNTRY_CODE = "BE";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Address instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.address = (Address) entityFactory.build(Address.class);
    this.countryDto = (CountryDto) entityFactory.build(CountryDto.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    address.setId(ID);
    assertEquals(ID, address.getId());
  }

  @Test
  public void testSetAndGetStreetTC1() {
    address.setStreet(STREET);
    assertEquals(STREET, address.getStreet());
  }

  @Test
  public void testSetAndGetNumberTC1() {
    address.setNumber(NUMBER);
    assertEquals(NUMBER, address.getNumber());
  }

  @Test
  public void testSetAndGetCityTC1() {
    address.setCity(CITY);
    assertEquals(CITY, address.getCity());
  }

  @Test
  public void testSetAndGetPostalCodeTC1() {
    address.setPostalCode(POSTALCODE);
    assertEquals(POSTALCODE, address.getPostalCode());
  }

  @Test
  public void testSetAndGetRegionTC1() {
    address.setRegion(REGION);
    assertEquals(REGION, address.getRegion());
  }

  @Test
  public void testSetAndGetCountryTC1() {
    address.setCountry(countryDto);
    assertEquals(countryDto, address.getCountry());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    address.setVersion(VERSION);
    assertEquals(VERSION, address.getVersion());
  }

  @Test
  public void testCheckDataIntegrityTC1() {
    setUpCorrectAddress();
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC2() {
    setUpCorrectAddress();
    address.setStreet(null);
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC3() {
    setUpCorrectAddress();
    address.setNumber(null);
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC4() {
    setUpCorrectAddress();
    address.setCity(null);
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC5() {
    setUpCorrectAddress();
    address.setPostalCode(null);
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC6() {
    setUpCorrectAddress();
    address.setRegion(null);
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC7() {
    setUpCorrectAddress();
    address.setCountry(null);
    address.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC8() {
    setUpCorrectAddress();
    address.getCountry().setCountryCode(null);
    address.checkDataIntegrity();
  }

  private void setUpCorrectAddress() {
    address.setId(ID);
    address.setStreet(STREET);
    address.setNumber(NUMBER);
    address.setCountry(countryDto);
    address.getCountry().setCountryCode(COUNTRY_CODE);
    address.setCity(CITY);
    address.setPostalCode(POSTALCODE);
    address.setRegion(REGION);
    address.setVersion(VERSION);
  }
}
