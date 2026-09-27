package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockAddressDao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;

public class TestAddressUcc {

  private EntityFactory entityFactory;
  private AddressDao addressDao;
  private AddressUcc addressUcc;
  private MockDtoFactory mockDtoFactory;
  private AddressDto address;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    addressDao = DependencyManager.getInstance(AddressDao.class);
    addressUcc = DependencyManager.getInstance(AddressUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    address = mockDtoFactory.getAddress();
  }

  /**
   * Cleans up the 'database'.
   */
  @AfterEach
  public void cleanUp() {
    ((MockAddressDao) addressDao).empty();
  }

  @Test
  public void testCreateTC1() {
    addressUcc.create(address);
    address.setId(1);
    assertEquals(address.getId(), (addressDao.findById(address.getId())).getId());
  }

  @Test
  public void testCreateTC2() {
    assertThrows(IllegalArgumentException.class, () -> {
      addressUcc.create(null);
    });
  }

  @Test
  public void testCreateTC3() {
    assertThrows(BusinessException.class, () -> {
      address.setNumber("The Number of the beast");
      addressUcc.create(address);
    });
  }

  @Test
  public void testCreateTC4() {
    assertThrows(RessourceNotFoundException.class, () -> {
      address.getCountry().setCountryCode("ZZ");
      addressUcc.create(address);
    });
  }

  @Test
  public void testEditTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      addressUcc.edit(null);
    });
  }

  @Test
  public void testEditTC2() {
    assertThrows(BusinessException.class, () -> {
      address.setNumber("V for Vendetta");
      addressUcc.edit(address);
    });
  }

  @Test
  public void testEditTC3() {
    assertThrows(RessourceNotFoundException.class, () -> {
      address.getCountry().setCountryCode("ZZ");
      addressUcc.edit(address);
    });
  }

  @Test
  public void testEditTC4() {
    assertThrows(RessourceNotFoundException.class, () -> {
      address.setId(69);
      addressUcc.edit(address);
    });
  }

  @Test
  public void testEditTC5() {
    addressUcc.create(address);
    addressUcc.edit(address);
    assertEquals(2, addressDao.findById(address.getId()).getVersion());
  }

}
