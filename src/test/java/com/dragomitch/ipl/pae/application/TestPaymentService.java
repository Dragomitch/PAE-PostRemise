package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryPaymentRepository;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.in.PaymentService;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestPaymentService {

  @Autowired private ApplicationContext context;

  private PaymentRepository paymentRepository;
  private PaymentService paymentService;
  private MockDtoFactory mockDtoFactory;
  private Payment payment1;
  private Payment payment2;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    paymentRepository = context.getBean(PaymentRepository.class);
    paymentService = context.getBean(PaymentService.class);
    mockDtoFactory = new MockDtoFactory();
    payment1 = mockDtoFactory.getPayment();
    payment2 = mockDtoFactory.getPayment();
    ((InMemoryPaymentRepository) paymentRepository).addPayment(payment1);
    ((InMemoryPaymentRepository) paymentRepository).addPayment(payment2);
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryPaymentRepository) paymentRepository).empty();
  }

  @Test
  public void testShowAllTC1() {
    List<Payment> payments = paymentService.showAll(Role.PROFESSOR);
    assertEquals(2, payments.size());
  }

  @Test
  public void testShowAllTC2() {
    assertThrows(
        InsufficientPermissionException.class,
        () -> {
          paymentService.showAll(Role.STUDENT);
        });
  }
}
