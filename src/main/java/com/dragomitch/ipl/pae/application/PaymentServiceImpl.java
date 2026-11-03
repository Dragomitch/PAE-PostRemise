package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.PaymentDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import com.dragomitch.ipl.pae.domain.port.in.PaymentService;

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
  public List<PaymentDto> showAll(String userRole) {
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    return paymentRepository.findAll();
  }

}
