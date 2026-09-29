package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/payments")
public class PaymentController {

  private final PaymentUcc paymentUcc;

  public PaymentController(PaymentUcc paymentUcc) {
    this.paymentUcc = paymentUcc;
  }

  /** Every payment, wrapped in {@code {"data": [...]}}. */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<PaymentDto> showAll(CurrentUser currentUser) {
    return new DataResponse<>(paymentUcc.showAll(currentUser.role()));
  }
}
