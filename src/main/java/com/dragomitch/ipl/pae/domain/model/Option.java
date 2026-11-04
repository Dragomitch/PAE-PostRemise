package com.dragomitch.ipl.pae.domain.model;

/** An option (study programme of the school), identified by its 3-character code. */
public class Option {

  private String code;
  private String name;
  private int version;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
