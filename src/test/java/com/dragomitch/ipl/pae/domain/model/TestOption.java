package com.dragomitch.ipl.pae.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestOption {

  @Autowired private ApplicationContext context;

  private static final String CODE = "BIN";
  private static final String NAME = "Bachelier en informatique de gestion";
  private static final int VERSION = 1;

  private Option option;

  /** Creates a new Option instance. */
  @BeforeEach
  public void setUp() throws Exception {
    this.option = new Option();
  }

  @Test
  public void testSetAndGetCodeTC1() {
    option.setCode(CODE);
    assertEquals(CODE, option.getCode());
  }

  @Test
  public void testSetAndGetNameTC1() {
    option.setName(NAME);
    assertEquals(NAME, option.getName());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    option.setVersion(VERSION);
    assertEquals(VERSION, option.getVersion());
  }
}
