package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.domain.dto.PaymentDto;
import com.dragomitch.ipl.pae.domain.port.in.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "The payments of the mobilities (professors only).")
@RestController
@RequestMapping(ApiPaths.BASE + "/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  /** Every payment, wrapped in {@code {"data": [...]}}. */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<PaymentDto> showAll(CurrentUser currentUser) {
    return new DataResponse<>(paymentService.showAll(currentUser.role()));
  }
}
