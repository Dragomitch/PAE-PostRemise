package java.business;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProgramme {

  private static final int ID = 1;
  private static final String PROGRAMMENAME = "Erasmus+";
  private static final String EXTERNALSOFTNAME = "Mobility Tool";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private ProgrammeDto programme;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Programme instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    programme.setId(ID);
    assertEquals(ID, programme.getId());
  }

  @Test
  public void testSetAndGetProgrammeNameTC1() {
    programme.setProgrammeName(PROGRAMMENAME);
    assertEquals(PROGRAMMENAME, programme.getProgrammeName());
  }

  @Test
  public void testSetAndGetExternalSoftNameTC1() {
    programme.setExternalSoftName(EXTERNALSOFTNAME);
    assertEquals(EXTERNALSOFTNAME, programme.getExternalSoftName());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    programme.setVersion(VERSION);
    assertEquals(VERSION, programme.getVersion());
  }
}
