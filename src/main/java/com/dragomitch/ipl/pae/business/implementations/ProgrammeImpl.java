package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.Programme;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;

import com.owlike.genson.annotation.JsonIgnore;

import java.io.Serializable;

@DaoClass(ProgrammeDao.class)
public class ProgrammeImpl implements Programme, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private String programName;
  private String externalSoftName;
  @JsonIgnore
  private int version;

  @Override
  public int getId() {
    return this.id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String getProgrammeName() {
    return this.programName;
  }

  @Override
  public void setProgrammeName(String programmeName) {
    this.programName = programmeName;
  }

  @Override
  public String getExternalSoftName() {
    return this.externalSoftName;
  }

  @Override
  public void setExternalSoftName(String externalSoftName) {
    this.externalSoftName = externalSoftName;
  }

  @Override
  @JsonIgnore
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public void checkDataIntegrity() {
    if (id < 0) {
      throw new IllegalArgumentException();
    }
    checkString(programName);
    checkString(externalSoftName);
  }

}
