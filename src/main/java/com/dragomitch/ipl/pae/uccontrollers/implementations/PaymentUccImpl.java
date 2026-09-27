package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class PaymentUccImpl implements PaymentUcc {

  private final PaymentDao paymentDao;

  PaymentUccImpl(PaymentDao paymentDao) {
    this.paymentDao = paymentDao;
  }

  @Override
  public List<PaymentDto> showAll(String userRole) {
    checkString(userRole);
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    return paymentDao.findAll();
  }

}
