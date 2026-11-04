package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryAddressRepository;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.AddressService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestAddressService {

  @Autowired private ApplicationContext context;

  private AddressRepository addressRepository;
  private AddressService addressService;
  private MockDtoFactory mockDtoFactory;
  private Address address;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    addressRepository = context.getBean(AddressRepository.class);
    addressService = context.getBean(AddressService.class);
    mockDtoFactory = new MockDtoFactory();
    address = mockDtoFactory.getAddress();
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryAddressRepository) addressRepository).empty();
  }

  @Test
  public void testCreateTC1() {
    addressService.create(address);
    address.setId(1);
    assertEquals(
        address.getId(), (addressRepository.findById(address.getId()).orElse(null)).getId());
  }

  @Test
  public void testCreateTC4() {
    assertEquals(
        ErrorCode.UNKNOWN_COUNTRY,
        Violations.errorCodeOf(() -> {
          address.getCountry().setCountryCode("ZZ");
          addressService.create(address);
        }));
  }

  @Test
  public void testEditTC3() {
    assertEquals(
        ErrorCode.UNKNOWN_COUNTRY,
        Violations.errorCodeOf(() -> {
          address.getCountry().setCountryCode("ZZ");
          addressService.edit(address);
        }));
  }

  @Test
  public void testEditTC4() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          address.setId(69);
          addressService.edit(address);
        });
  }

  @Test
  public void testEditTC5() {
    addressService.create(address);
    addressService.edit(address);
    assertEquals(2, addressRepository.findById(address.getId()).orElseThrow().getVersion());
  }
}
