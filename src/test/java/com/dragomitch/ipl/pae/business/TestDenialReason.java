package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestDenialReason {

  @Autowired
  private ApplicationContext context;

  private static final int ID = 1;
  private static final String REASON = "Mon ours en peluche est mort";

  private EntityFactory entityFactory;
  private DenialReasonDto denialReason;

  /**
   * Creates a new DenialReason instance.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
    this.denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
  }

  @Test
  public void testSetAndGetIdTC1() {
    denialReason.setId(ID);
    assertEquals(ID, denialReason.getId());
  }

  @Test
  public void testSetAndGetReasonTC1() {
    denialReason.setReason(REASON);
    assertEquals(REASON, denialReason.getReason());
  }

  @Test
  public void testSetAndGetVersionTC1() {
    denialReason.setVersion(ID);
    assertEquals(ID, denialReason.getVersion());
  }

}
