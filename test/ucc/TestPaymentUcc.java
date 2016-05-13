package ucc;

import static org.junit.Assert.assertEquals;

import business.EntityFactory;
import business.dto.PaymentDto;
import business.dto.UserDto;
import main.ContextManager;
import main.DependencyManager;
import persistence.PaymentDao;
import persistence.mocks.MockPaymentDao;
import presentation.exceptions.InsufficientPermissionException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

public class TestPaymentUcc {

  private EntityFactory entityFactory;
  private PaymentDao paymentDao;
  private PaymentUcc paymentUcc;
  private MockDtoFactory mockDtoFactory;
  private PaymentDto payment1;
  private PaymentDto payment2;


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
    paymentDao = DependencyManager.getInstance(PaymentDao.class);
    paymentUcc = DependencyManager.getInstance(PaymentUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    payment1 = mockDtoFactory.getPayment();
    payment2 = mockDtoFactory.getPayment();
    ((MockPaymentDao) paymentDao).addPayment(payment1);
    ((MockPaymentDao) paymentDao).addPayment(payment2);
  }

  /**
   * Cleans up the 'database'.
   */
  @After
  public void cleanUp() {
    ((MockPaymentDao) paymentDao).empty();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testShowAllTC1() {
    Object payments = paymentUcc.showAll(UserDto.ROLE_PROFESSOR).get("data");
    assertEquals(2, ((ArrayList<PaymentDto>) payments).size());
  }

  @SuppressWarnings("unchecked")
  @Test(expected = InsufficientPermissionException.class)
  public void testShowAllTC2() {
    Object payments = paymentUcc.showAll(UserDto.ROLE_STUDENT).get("data");
    assertEquals(2, ((ArrayList<PaymentDto>) payments).size());
  }


}
