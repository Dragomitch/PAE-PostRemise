package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockAddressDao;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;

public class TestAddressUcc {

  private EntityFactory entityFactory;
  private AddressDao addressDao;
  private AddressUcc addressUcc;
  private MockDtoFactory mockDtoFactory;
  private AddressDto address;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
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
  @After
  public void cleanUp() {
    ((MockAddressDao) addressDao).empty();
  }

  @Test
  public void testCreateTC1() {
    addressUcc.create(address);
    address.setId(1);
    assertEquals(address.getId(), (addressDao.findById(address.getId())).getId());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateTC2() {
    addressUcc.create(null);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC3() {
    address.setNumber("The Number of the beast");
    addressUcc.create(address);
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testCreateTC4() {
    address.getCountry().setCountryCode("ZZ");
    addressUcc.create(address);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEditTC1() {
    addressUcc.edit(null);
  }

  @Test(expected = BusinessException.class)
  public void testEditTC2() {
    address.setNumber("V for Vendetta");
    addressUcc.edit(address);
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testEditTC3() {
    address.getCountry().setCountryCode("ZZ");
    addressUcc.edit(address);
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testEditTC4() {
    address.setId(69);
    addressUcc.edit(address);
  }

  @Test
  public void testEditTC5() {
    addressUcc.create(address);
    addressUcc.edit(address);
    assertEquals(2, addressDao.findById(address.getId()).getVersion());
  }

}
