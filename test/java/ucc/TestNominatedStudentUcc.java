package java.ucc;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.context.ErrorManager;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockNominatedStudentDao;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;

public class TestNominatedStudentUcc {

  private EntityFactory entityFactory;
  private NominatedStudentDao nominatedStudentDao;
  private NominatedStudentUcc nominatedStudentUcc;
  private MockDtoFactory mockDtoFactory;
  private NominatedStudentDto nominatedStud;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
  public void setUp() {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    nominatedStudentDao = DependencyManager.getInstance(NominatedStudentDao.class);
    nominatedStudentUcc = DependencyManager.getInstance(NominatedStudentUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    nominatedStud = mockDtoFactory.getNominatedStudent();
    ErrorManager.load();
  }

  /**
   * Cleans up the 'database'.
   */
  @After
  public void cleanUp() {
    ((MockNominatedStudentDao) nominatedStudentDao).empty();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateTC1() {
    nominatedStudentUcc.create(null, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC2() {
    nominatedStudentDao.create(nominatedStud);
    nominatedStudentUcc.create(nominatedStud, 1, UserDto.ROLE_PROFESSOR);
    assertEquals(1, nominatedStudentDao.findAll().size());
  }
}
