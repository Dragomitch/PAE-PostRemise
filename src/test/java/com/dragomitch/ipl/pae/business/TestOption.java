package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestOption {

  private static final String CODE = "BIN";
  private static final String NAME = "Bachelier en informatique de gestion";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private OptionDto option;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new Option instance.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
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
