package com.dragomitch.ipl.pae.domain.model;

/** A mobility programme (reference data: Erasmus+, Erabel...). */
public class Programme {

  private int id;
  private String programmeName;
  private String externalSoftName;
  private int version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getProgrammeName() {
    return programmeName;
  }

  public void setProgrammeName(String programmeName) {
    this.programmeName = programmeName;
  }

  public String getExternalSoftName() {
    return externalSoftName;
  }

  public void setExternalSoftName(String externalSoftName) {
    this.externalSoftName = externalSoftName;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
