package com.dragomitch.ipl.pae.domain.model;

/** A reason given by a professor to reject a mobility choice or to cancel a mobility. */
public class DenialReason {

  private int id;
  private String reason;
  private int version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
