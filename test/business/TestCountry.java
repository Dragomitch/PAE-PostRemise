package business;

import static org.junit.Assert.assertEquals;

import business.dto.CountryDto;
import business.dto.ProgrammeDto;
import main.ContextManager;
import main.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCountry {

  private static final String COUNTRYCODE = "KZ";
  private static final String NAME = "Kazakhstan";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private CountryDto country;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Country instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.country = (CountryDto) entityFactory.build(CountryDto.class);
  }

  @Test
  public void testSetAndGetCountryCodeTC1() {
    country.setCountryCode(COUNTRYCODE);
    assertEquals(COUNTRYCODE, country.getCountryCode());
  }

  @Test
  public void testSetAndGetNameTC1() {
    country.setName(NAME);
    assertEquals(NAME, country.getName());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    country.setVersion(VERSION);
    assertEquals(VERSION, country.getVersion());
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    country.setProgramme(programme);
    assertEquals(programme, country.getProgramme());
  }

}
