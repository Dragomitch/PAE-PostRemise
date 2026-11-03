package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.dto.PaymentDto;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentService {

  /**
   * Ask the PaymentRepository to get all payments in the database.
   *
   * @param userRole the role of the requester, only professors may list payments
   * @return a list of all the payments found in the database
   */
  List<PaymentDto> showAll(@NotBlank String userRole);
}
