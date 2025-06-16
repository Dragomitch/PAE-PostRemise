package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.dragomitch.ipl.pae.business.dto.CountryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AddressTest {

  @Autowired
  private DtoFactory dtoFactory;

  @Test
  void factoryProducesPrototype() {
    Address first = dtoFactory.create(Address.class);
    Address second = dtoFactory.create(Address.class);
    assertNotSame(first, second);
  }

  @Test
  void validAddressPassesIntegrityCheck() {
    Address address = dtoFactory.create(Address.class);
    CountryDto country = dtoFactory.create(CountryDto.class);
    country.setCountryCode("BE");
    address.setStreet("street");
    address.setNumber("1");
    address.setCountry(country);
    address.setCity("City");
    address.setPostalCode("1000");
    address.setRegion("Region");
    assertDoesNotThrow(address::checkDataIntegrity);
  }
}
