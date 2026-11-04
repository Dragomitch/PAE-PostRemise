package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryPaymentRepository implements PaymentRepository, ResettableRepository {

  private List<Payment> payments;

  public InMemoryPaymentRepository() {
    payments = new ArrayList<Payment>();
  }

  @Override
  public List<Payment> findAll() {
    return payments;
  }

  public void addPayment(Payment payment) {
    payments.add(payment);
  }

  @Override
  public void empty() {
    payments = new ArrayList<Payment>();
  }
}
