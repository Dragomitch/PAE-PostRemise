package business.implementations;

import static utils.DataValidationUtils.checkString;

import business.Option;
import persistence.DaoClass;
import persistence.OptionDao;

import com.owlike.genson.annotation.JsonIgnore;

import java.io.Serializable;

@DaoClass(OptionDao.class)
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
