package com.dragomitch.ipl.pae.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JsonSerializerTest {

  @Autowired
  private JsonSerializer serializer;

  @Autowired
  private DtoFactory dtoFactory;

  @Test
  void serializationRoundTrip() {
    Address address = dtoFactory.create(Address.class);
    CountryDto country = dtoFactory.create(CountryDto.class);
    country.setCountryCode("BE");
    address.setStreet("street");
    address.setNumber("1");
    address.setCity("City");
    address.setCountry(country);
    String json = serializer.serialize(address);
    Address copy = (Address) serializer.deserialize(json, Address.class);
    assertEquals("street", copy.getStreet());
    assertEquals("BE", copy.getCountry().getCountryCode());
  }
}
