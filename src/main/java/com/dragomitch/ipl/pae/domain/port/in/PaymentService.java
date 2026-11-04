package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;

public interface PaymentService {

  /**
   * Ask the PaymentRepository to get all payments in the database.
   *
   * @param userRole the role of the requester, only professors may list payments
   * @return a list of all the payments found in the database
   */
  List<Payment> showAll(Role userRole);
}
