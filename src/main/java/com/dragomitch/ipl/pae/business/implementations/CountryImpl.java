package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.Country;
import com.dragomitch.ipl.pae.business.Programme;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DaoClass;

import java.io.Serializable;

@DaoClass(CountryDao.class)
class CountryImpl implements Country, Serializable {

  private static final long serialVersionUID = 1L;

  private String countryCode;
  private String name;
  private ProgrammeDto programme;
  private int version;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ProgrammeDto getProgramme() {
    return programme;
  }

  @Override
  public void setProgramme(ProgrammeDto programme) {
    this.programme = programme;
  }

  @Override
  public String getCountryCode() {
    return countryCode;
  }

  @Override
  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
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
    checkString(countryCode);
    checkString(name);
    ((Programme) programme).checkDataIntegrity();
  }

}
