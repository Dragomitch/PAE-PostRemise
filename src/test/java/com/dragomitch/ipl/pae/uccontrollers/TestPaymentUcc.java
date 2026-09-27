package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPaymentDao;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;

import java.util.ArrayList;

public class TestPaymentUcc {

  private EntityFactory entityFactory;
  private PaymentDao paymentDao;
  private PaymentUcc paymentUcc;
  private MockDtoFactory mockDtoFactory;
  private PaymentDto payment1;
  private PaymentDto payment2;


  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
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
  @AfterEach
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
  @Test
  public void testShowAllTC2() {
    assertThrows(InsufficientPermissionException.class, () -> {
      Object payments = paymentUcc.showAll(UserDto.ROLE_STUDENT).get("data");
      assertEquals(2, ((ArrayList<PaymentDto>) payments).size());
    });
  }


}
