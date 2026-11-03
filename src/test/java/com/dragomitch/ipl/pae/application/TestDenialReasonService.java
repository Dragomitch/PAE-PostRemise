package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryDenialReasonRepository;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestDenialReasonService {

  @Autowired private ApplicationContext context;
  private DenialReasonRepository denialReasonRepository;
  private DenialReasonService denialReasonService;
  private MockDtoFactory mockDtoFactory;
  private DenialReason denialReason;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    denialReasonRepository = context.getBean(DenialReasonRepository.class);
    denialReasonService = context.getBean(DenialReasonService.class);
    mockDtoFactory = new MockDtoFactory();
    denialReason = mockDtoFactory.getDenialReason();
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryDenialReasonRepository) denialReasonRepository).empty();
  }

  @Test
  public void testShowAllTC1() {
    ((InMemoryDenialReasonRepository) denialReasonRepository).empty();
    assertEquals(0, denialReasonService.showAll().size(), "The database should be empty");
  }

  @Test
  public void testShowAllTC2() {
    denialReasonService.create(denialReason);
    assertEquals(
        1, denialReasonService.showAll().size(), "The database should countain 1 denial reason");
  }

  @Test
  public void testCreateTC2() {
    denialReasonService.create(denialReason);
    assertEquals(
        1, denialReasonService.showAll().size(), "The database should contain only 1 denialreason");
  }

  @Test
  public void testCreateTC3() {
    denialReasonService.create(denialReason);
    assertEquals(1, denialReason.getId());
  }

  @Test
  public void testEditTC3() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          denialReasonService.edit(1, denialReason);
        });
  }

  @Test
  public void testEditTC4() {
    denialReasonService.create(denialReason);
    String newOne = "Autre soEver";
    denialReason.setReason(newOne);
    denialReasonService.edit(1, denialReason);
    assertEquals(
        newOne,
        denialReasonService.showAll().get(0).getReason(),
        "The update is not correctly done for the field reason");
  }

  @Test
  public void editUpdatesTheReasonIdentifiedByThePath() {
    denialReasonService.create(denialReason);
    DenialReason second = mockDtoFactory.getDenialReason();
    second.setReason("Second");
    denialReasonService.create(second);
    DenialReason body = mockDtoFactory.getDenialReason();
    body.setId(1);
    body.setReason("Edited");

    DenialReason edited = denialReasonService.edit(2, body);

    assertEquals(2, edited.getId());
    assertEquals("no reason whatsoever", denialReasonRepository.findById(1).getReason());
    assertEquals("Edited", denialReasonRepository.findById(2).getReason());
  }

  @Test
  public void testEditTC5() {
    denialReasonService.create(denialReason);
    DenialReason reason = denialReasonService.edit(1, denialReason);
    assertEquals(1, reason.getVersion(), "The update is not correctly done for the version");
  }
}
