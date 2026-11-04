package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Payment;
import java.util.List;

public interface PaymentRepository {

  /**
   * Queries the database for all payments.
   *
   * @return all payments
   */
  List<Payment> findAll();
}
