package business;

import static org.junit.Assert.assertEquals;

import business.dto.OptionDto;
import main.ContextManager;
import main.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestOption {

  private static final String CODE = "BIN";
  private static final String NAME = "Bachelier en informatique de gestion";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private OptionDto option;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Option instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.option = (OptionDto) entityFactory.build(OptionDto.class);
  }

  @Test
  public void testSetAndGetCodeTC1() {
    option.setCode(CODE);
    assertEquals(CODE, option.getCode());
  }

  @Test
  public void testSetAndGetNameTC1() {
    option.setName(NAME);
    assertEquals(NAME, option.getName());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    option.setVersion(VERSION);;
    assertEquals(VERSION, option.getVersion());
  }

}
