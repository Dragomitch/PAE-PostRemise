package com.dragomitch.ipl.pae.business.dto;

public interface AddressDto extends Entity {

  String getStreet();

  void setStreet(String street);

  String getNumber();

  void setNumber(String number);

  CountryDto getCountry();

  void setCountry(CountryDto country);

  String getCity();

  void setCity(String city);

  String getPostalCode();

  void setPostalCode(String postalCode);

  String getRegion();

  void setRegion(String region);

}
