package java.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDocument {

  private static final int ID = 1;
  private static final String NAME = "Contrat de bourse";
  private static final char CATEGORY = 'D';
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private DocumentDto document;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Document instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    this.document = (DocumentDto) entityFactory.build(DocumentDto.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    document.setId(ID);
    assertEquals(ID, document.getId());
  }

  @Test
  public void testSetAndGetNameTC1() {
    document.setName(NAME);
    assertEquals(NAME, document.getName());
  }

  @Test
  public void testSetAndGetCategoryTC1() {
    document.setCategory(CATEGORY);
    assertEquals(CATEGORY, document.getCategory());
  }

  @Test
  public void testSetAndGetFilledInTC1() {
    document.setFilledIn(true);
    assertTrue(document.isFilledIn());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    document.setVersion(VERSION);
    assertEquals(VERSION, document.getVersion());
  }

}
