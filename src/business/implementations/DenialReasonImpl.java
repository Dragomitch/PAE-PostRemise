package business.implementations;

import static utils.DataValidationUtils.isAValidString;

import business.DenialReason;
import business.exceptions.BusinessException;
import business.exceptions.ErrorFormat;
import persistence.DaoClass;
import persistence.DenialReasonDao;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@DaoClass(DenialReasonDao.class)
class DenialReasonImpl implements DenialReason, Serializable {

  private static final long serialVersionUID = 1L;
  private int id;
  private String reason;
  private int version;

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String getReason() {
    return reason;
  }

  @Override
  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public void checkDataIntegrity() {
    List<Integer> violations = new LinkedList<Integer>();
    if (!isAValidString(reason)) {
      violations.add(ErrorFormat.INVALID_REASON_401);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
