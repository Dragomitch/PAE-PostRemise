package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.dto.PaymentDto;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryPaymentRepository implements PaymentRepository, ResettableRepository {

  private List<PaymentDto> payments;

  public InMemoryPaymentRepository() {
    payments = new ArrayList<PaymentDto>();
  }

  @Override
  public List<PaymentDto> findAll() {
    return payments;
  }

  public void addPayment(PaymentDto payment) {
    payments.add(payment);
  }

  @Override
  public void empty() {
    payments = new ArrayList<PaymentDto>();
  }
}
