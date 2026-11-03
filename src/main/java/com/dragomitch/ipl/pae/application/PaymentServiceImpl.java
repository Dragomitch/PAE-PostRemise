package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.in.PaymentService;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;

  PaymentServiceImpl(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Override
  public List<Payment> showAll(Role userRole) {
    if (userRole == Role.STUDENT) {
      throw new InsufficientPermissionException();
    }
    return paymentRepository.findAll();
  }
}
