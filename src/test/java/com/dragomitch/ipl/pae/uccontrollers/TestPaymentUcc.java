package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPaymentDao;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;

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
