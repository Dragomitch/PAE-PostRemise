package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidEmail;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.UserDao;

import com.owlike.genson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@DaoClass(UserDao.class)
class UserImpl implements User, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private String firstName;
  private String lastName;
  private String username;
  private String password;
  private String email;
  private OptionDto option;
  private LocalDateTime registrationDate;
  private String role; // Student or Professor
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
  public String getFirstName() {
    return firstName;
  }

  @Override
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @Override
  public String getLastName() {
    return lastName;
  }

  @Override
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  @JsonIgnore
  public String getPassword() {
    return password;
  }

  @Override
  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public OptionDto getOption() {
    return option;
  }

  @Override
  public void setOption(OptionDto option) {
    this.option = option;
  }

  @Override
  public LocalDateTime getRegistrationDate() {
    return registrationDate;
  }

  @Override
  public void setRegistrationDate(LocalDateTime registrationDate) {
    this.registrationDate = registrationDate;
  }

  @Override
  public void setRole(String role) {
    this.role = role;
  }

  @Override
  public String getRole() {
    return role;
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
    if (!isAValidString(username)) {
      violations.add(ErrorFormat.INVALID_USERNAME_203);
    } else if (username.length() > UserDao.USERNAME_MAX_LENGTH) {
      violations.add(ErrorFormat.USERNAME_MAX_LENGTH_OVERFLOW_213);
    }
    if (!isAValidString(lastName)) {
      violations.add(ErrorFormat.INVALID_LAST_NAME_202);
    } else if (lastName.length() > UserDao.LAST_NAME_MAX_LENGTH) {
      violations.add(ErrorFormat.LAST_NAME_MAX_LENGTH_OVERFLOW_212);
    }
    if (!isAValidString(firstName)) {
      violations.add(ErrorFormat.INVALID_FIRST_NAME_201);
    } else if (firstName.length() > UserDao.FIRST_NAME_MAX_LENGTH) {
      violations.add(ErrorFormat.FIRST_NAME_MAX_LENGTH_OVERFLOW_211);
    }
    if (!isAValidString(password)) {
      violations.add(ErrorFormat.INVALID_PASSWORD_205);
    } else if (password.length() > UserDao.PASSWORD_MAX_LENGTH) {
      violations.add(ErrorFormat.PASSWORD_MAX_LENGTH_OVERFLOW_214);
    }
    if (!isAValidEmail(email)) {
      violations.add(ErrorFormat.INVALID_EMAIL_206);
    } else if (email.length() > UserDao.EMAIL_MAX_LENGTH) {
      violations.add(ErrorFormat.EMAIL_MAX_LENGTH_OVERFLOW_215);
    }
    if (!isAValidObject(option)) {
      violations.add(ErrorFormat.INVALID_OPTION_208);
    } else if (!isAValidString(option.getCode())) {
      violations.add(ErrorFormat.INVALID_OPTION_CODE_209);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
