package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestProgramme {

  @Autowired
  private ApplicationContext context;

  private static final int ID = 1;
  private static final String PROGRAMMENAME = "Erasmus+";
  private static final String EXTERNALSOFTNAME = "Mobility Tool";
  private static final int VERSION = 1;

  private EntityFactory entityFactory;
  private ProgrammeDto programme;

  /**
   * Creates a new Programme instance.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
    this.programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    programme.setId(ID);
    assertEquals(ID, programme.getId());
  }

  @Test
  public void testSetAndGetProgrammeNameTC1() {
    programme.setProgrammeName(PROGRAMMENAME);
    assertEquals(PROGRAMMENAME, programme.getProgrammeName());
  }

  @Test
  public void testSetAndGetExternalSoftNameTC1() {
    programme.setExternalSoftName(EXTERNALSOFTNAME);
    assertEquals(EXTERNALSOFTNAME, programme.getExternalSoftName());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    programme.setVersion(VERSION);
    assertEquals(VERSION, programme.getVersion());
  }
}
