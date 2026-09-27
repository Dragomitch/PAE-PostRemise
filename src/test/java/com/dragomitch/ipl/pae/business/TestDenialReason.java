package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDenialReason {

  private static final int ID = 1;
  private static final String REASON = "Mon ours en peluche est mort";

  private EntityFactory entityFactory;
  private DenialReasonDto denialReason;

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new DenialReason instance.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
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
