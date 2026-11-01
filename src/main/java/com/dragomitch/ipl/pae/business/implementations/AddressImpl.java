package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.dto.CountryDto;

import java.io.Serializable;

class AddressImpl implements Address, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private String street;
  private String number;
  private CountryDto country;
  private String city;
  private String postalCode;
  private String region;
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
  public String getStreet() {
    return street;
  }

  @Override
  public void setStreet(String street) {
    this.street = street;
  }

  @Override
  public String getNumber() {
    return number;
  }

  @Override
  public void setNumber(String number) {
    this.number = number;
  }

  @Override
  public CountryDto getCountry() {
    return country;
  }

  @Override
  public void setCountry(CountryDto country) {
    this.country = country;
  }

  @Override
  public String getCity() {
    return city;
  }

  @Override
  public void setCity(String city) {
    this.city = city;
  }

  @Override
  public String getPostalCode() {
    return postalCode;
  }

  @Override
  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  @Override
  public String getRegion() {
    return region;
  }

  @Override
  public void setRegion(String region) {
    this.region = region;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }


}
