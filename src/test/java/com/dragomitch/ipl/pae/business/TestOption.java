package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestOption {

  @Autowired
  private ApplicationContext context;

  private static final String CODE = "BIN";
  private static final String NAME = "Bachelier en informatique de gestion";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private OptionDto option;

  /**
   * Creates a new Option instance.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
    this.option = (OptionDto) entityFactory.build(OptionDto.class);
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
    option.setVersion(VERSION);;
    assertEquals(VERSION, option.getVersion());
  }

}
