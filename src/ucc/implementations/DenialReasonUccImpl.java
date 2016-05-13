package ucc.implementations;

import static utils.DataValidationUtils.checkObject;
import static utils.DataValidationUtils.checkPositive;
import static utils.DataValidationUtils.isAValidString;

import business.DenialReason;
import business.dto.DenialReasonDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import business.exceptions.ErrorFormat;
import business.exceptions.RessourceNotFoundException;
import main.annotations.Inject;
import persistence.DenialReasonDao;
import presentation.annotations.HttpParameter;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.enums.HttpMethod;
import ucc.DenialReasonUcc;
import ucc.UnitOfWork;

import java.util.LinkedList;
import java.util.List;

class DenialReasonUccImpl implements DenialReasonUcc {

  private DenialReasonDao denialReasonDao;
  private UnitOfWork unitOfWork;

  @Inject
  DenialReasonUccImpl(DenialReasonDao denialReasonDao, UnitOfWork unitOfWork) {
    this.denialReasonDao = denialReasonDao;
    this.unitOfWork = unitOfWork;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.POST, template = "/denialreasons")
  public DenialReasonDto create(@HttpParameter("reason") DenialReasonDto denialReason) {
    checkObject(denialReason);
    checkDataIntegrity(denialReason);
    try {
      unitOfWork.startTransaction();
      denialReason = denialReasonDao.create(denialReason);
      unitOfWork.commit();
      return denialReason;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/denialreasons")
  public List<DenialReasonDto> showAll() {
    try {
      unitOfWork.startTransaction();
      List<DenialReasonDto> denialReasonList = denialReasonDao.findAll();
      unitOfWork.commit();
      return denialReasonList;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/denialreasons/{id}")
  public DenialReasonDto edit(@PathParameter("id") int id, DenialReasonDto denialReason) {
    checkPositive(id);
    checkObject(denialReason);
    try {
      unitOfWork.startTransaction();
      if (denialReasonDao.findById(id) == null) {
        throw new RessourceNotFoundException();
      }
      checkDataIntegrity(denialReason);
      denialReasonDao.update(denialReason);
      unitOfWork.commit();
      return denialReason;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  /**
   * Verify the validity of the information received.
   * 
   * @param denialReason the DenialReasonDto we verify.
   */
  private void checkDataIntegrity(DenialReasonDto denialReason) {
    List<Integer> violations = new LinkedList<Integer>();
    if (denialReason == null) {
      throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_DENIAL_REASON_NULL_134);
    }
    try {
      ((DenialReason) denialReason).checkDataIntegrity();
    } catch (BusinessException ex) {
      List<ErrorFormat> errors = ex.getError().getDetails();
      for (ErrorFormat oneError : errors) {
        violations.add(oneError.getErrorCode());
      }
    }
    if (isAValidString(denialReason.getReason())
        && denialReason.getReason().length() > DenialReasonDao.MAX_LENGTH_REASON) {
      violations.add(ErrorFormat.MAX_LENGTH_REASON_OVERFLOW_402);
    }

    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }

}
