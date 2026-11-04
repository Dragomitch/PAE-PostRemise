package com.dragomitch.ipl.pae.domain.model;

/** A country (reference data), identified by its ISO 3166-1 alpha-2 code. */
public class Country {

  private String countryCode;
  private String name;
  private Programme programme;
  private int version;

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Programme getProgramme() {
    return programme;
  }

  public void setProgramme(Programme programme) {
    this.programme = programme;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
