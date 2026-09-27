package java.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCountry {

  private static final String COUNTRYCODE = "KZ";
  private static final String NAME = "Kazakhstan";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private CountryDto country;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Country instance.
   */
  @BeforeEach
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
