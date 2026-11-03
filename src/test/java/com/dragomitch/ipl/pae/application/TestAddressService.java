package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryAddressRepository;
import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.AddressService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestAddressService {

  @Autowired private ApplicationContext context;

  private EntityFactory entityFactory;
  private AddressRepository addressRepository;
  private AddressService addressService;
  private MockDtoFactory mockDtoFactory;
  private AddressDto address;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    addressRepository = context.getBean(AddressRepository.class);
    addressService = context.getBean(AddressService.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
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
    assertEquals(address.getId(), (addressRepository.findById(address.getId())).getId());
  }

  @Test
  public void testCreateTC2() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          addressService.create(null);
        });
  }

  @Test
  public void testCreateTC3() {
    assertEquals(
        List.of("create.address.number:Size"),
        Violations.thrownBy(() -> {
          address.setNumber("The Number of the beast");
          addressService.create(address);
        }));
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
  public void testEditTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          addressService.edit(null);
        });
  }

  @Test
  public void testEditTC2() {
    assertEquals(
        List.of("edit.address.number:Size"),
        Violations.thrownBy(() -> {
          address.setNumber("V for Vendetta");
          addressService.edit(address);
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
    assertEquals(2, addressRepository.findById(address.getId()).getVersion());
  }
}
