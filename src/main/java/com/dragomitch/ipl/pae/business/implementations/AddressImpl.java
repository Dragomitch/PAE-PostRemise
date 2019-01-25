package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.DaoClass;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@DaoClass(AddressDao.class)
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

  @Override
  public void checkDataIntegrity() {
    List<Integer> violations = new LinkedList<Integer>();
    if (!isAValidString(street)) {
      violations.add(ErrorFormat.INVALID_STREET_801);
    }
    if (!isAValidString(number)) {
      violations.add(ErrorFormat.INVALID_STREET_NUMBER_802);
    }
    if (!isAValidObject(country)) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_COUNTRY_NULL_139);
    } else if (!isAValidString(country.getCountryCode())) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_COUNTRY_CODE_900);
    }
    if (!isAValidString(city)) {
      violations.add(ErrorFormat.INVALID_CITY_803);
    }
    if (!isAValidString(postalCode)) {
      violations.add(ErrorFormat.INVALID_POSTAL_CODE_804);
    }
    if (!isAValidObject(region)) {
      violations.add(ErrorFormat.INVALID_REGION_805);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }

}
