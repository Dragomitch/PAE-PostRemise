package com.dragomitch.ipl.pae.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestCountry {

  @Autowired private ApplicationContext context;

  private static final String COUNTRYCODE = "KZ";
  private static final String NAME = "Kazakhstan";
  private static final int VERSION = 1;

  private Country country;

  /** Creates a new Country instance. */
  @BeforeEach
  public void setUp() throws Exception {
    this.country = new Country();
  }

  @Test
  public void testSetAndGetCountryCodeTC1() {
    country.setCountryCode(COUNTRYCODE);
    assertEquals(COUNTRYCODE, country.getCountryCode());
  }

  @Test
  public void testSetAndGetNameTC1() {
    country.setName(NAME);
    assertEquals(NAME, country.getName());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    country.setVersion(VERSION);
    assertEquals(VERSION, country.getVersion());
  }

  @Test
  public void testSetAndGetProgrammeTC1() {
    Programme programme = new Programme();
    country.setProgramme(programme);
    assertEquals(programme, country.getProgramme());
  }
}
