package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.Option;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

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


}
