package com.dragomitch.ipl.pae.uccontrollers;

import java.util.Map;

public interface PaymentUcc {

  /**
   * Ask the PaymentDao to get all payments in the database.
   * 
   * @return a list of all the payments found in the database
   */
  Map<String, Object> showAll(String userRole);

}
