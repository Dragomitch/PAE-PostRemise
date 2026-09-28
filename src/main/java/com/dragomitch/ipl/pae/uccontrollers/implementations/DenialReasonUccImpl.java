package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositive;

import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;

import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DenialReasonUccImpl implements DenialReasonUcc {

  private final DenialReasonDao denialReasonDao;

  DenialReasonUccImpl(DenialReasonDao denialReasonDao) {
    this.denialReasonDao = denialReasonDao;
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    checkObject(denialReason);
    checkDataIntegrity(denialReason);
    return denialReasonDao.create(denialReason);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DenialReasonDto> showAll() {
    return denialReasonDao.findAll();
  }

  @Override
  public DenialReasonDto edit(int id, DenialReasonDto denialReason) {
    checkPositive(id);
    checkObject(denialReason);
    if (denialReasonDao.findById(id) == null) {
      throw new RessourceNotFoundException();
    }
    denialReason.setId(id);
    checkDataIntegrity(denialReason);
    denialReasonDao.update(denialReason);
    return denialReason;
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
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }

}
