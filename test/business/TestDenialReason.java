package business;

import static org.junit.Assert.assertEquals;

import business.dto.DenialReasonDto;
import main.ContextManager;
import main.DependencyManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDenialReason {

  private static final int ID = 1;
  private static final String REASON = "Mon ours en peluche est mort";

  private EntityFactory entityFactory;
  private DenialReasonDto denialReason;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new DenialReason instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    denialReason.setId(ID);
    assertEquals(ID, denialReason.getId());
  }

  @Test
  public void testSetAndGetReasonTC1() {
    denialReason.setReason(REASON);
    assertEquals(REASON, denialReason.getReason());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    denialReason.setVersion(ID);
    assertEquals(ID, denialReason.getVersion());
  }

}
