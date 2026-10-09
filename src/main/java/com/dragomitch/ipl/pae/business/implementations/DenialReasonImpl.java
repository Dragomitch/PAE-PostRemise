package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

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
    } else if (reason.length() > DenialReasonDao.MAX_LENGTH_REASON) {
      violations.add(ErrorFormat.MAX_LENGTH_REASON_OVERFLOW_402);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
