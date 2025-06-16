package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.annotations.SessionParameter;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;
import com.dragomitch.ipl.pae.uccontrollers.UnitOfWork;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentUccImpl implements PaymentUcc {

  private PaymentDao paymentDao;
  private UnitOfWork unitOfWork;

  
  public PaymentUccImpl(PaymentDao paymentDao, UnitOfWork unitOfWork) {
    this.paymentDao = paymentDao;
    this.unitOfWork = unitOfWork;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/payments")
  public Map<String, Object> showAll(@SessionParameter("userRole") String userRole) {
    checkString(userRole);
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    try {
      unitOfWork.startTransaction();
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("data", paymentDao.findAll());
      unitOfWork.commit();
      return map;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

}
