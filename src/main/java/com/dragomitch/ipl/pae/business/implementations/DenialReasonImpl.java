package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.DenialReason;

import java.io.Serializable;

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

}
