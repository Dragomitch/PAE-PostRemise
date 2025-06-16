package com.dragomitch.ipl.pae.business.implementations;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.Option;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.OptionDao;

import com.owlike.genson.annotation.JsonIgnore;

import java.io.Serializable;

@DaoClass(OptionDao.class)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class OptionImpl implements Option, Serializable {

  private static final long serialVersionUID = 1L;

  private String code;
  private String name;
  @JsonIgnore
  private int version;

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
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
    checkString(code);
    checkString(name);
  }

}
