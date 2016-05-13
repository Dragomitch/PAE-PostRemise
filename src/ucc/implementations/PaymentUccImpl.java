package ucc.implementations;

import static utils.DataValidationUtils.checkString;

import business.dto.UserDto;
import main.annotations.Inject;
import persistence.PaymentDao;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.annotations.SessionParameter;
import presentation.enums.HttpMethod;
import presentation.exceptions.InsufficientPermissionException;
import ucc.PaymentUcc;
import ucc.UnitOfWork;

import java.util.HashMap;
import java.util.Map;

public class PaymentUccImpl implements PaymentUcc {

  private PaymentDao paymentDao;
  private UnitOfWork unitOfWork;

  @Inject
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
