package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPaymentDao;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;

import java.util.List;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestPaymentUcc {

  @Autowired
  private ApplicationContext context;

  private EntityFactory entityFactory;
  private PaymentDao paymentDao;
  private PaymentUcc paymentUcc;
  private MockDtoFactory mockDtoFactory;
  private PaymentDto payment1;
  private PaymentDto payment2;


  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    paymentDao = context.getBean(PaymentDao.class);
    paymentUcc = context.getBean(PaymentUcc.class);
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

  @Test
  public void testShowAllTC1() {
    List<PaymentDto> payments = paymentUcc.showAll(UserDto.ROLE_PROFESSOR);
    assertEquals(2, payments.size());
  }

  @Test
  public void testShowAllTC2() {
    assertThrows(InsufficientPermissionException.class, () -> {
      paymentUcc.showAll(UserDto.ROLE_STUDENT);
    });
  }


}
