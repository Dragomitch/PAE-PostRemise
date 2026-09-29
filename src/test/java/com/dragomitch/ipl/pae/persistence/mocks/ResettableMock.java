package com.dragomitch.ipl.pae.persistence.mocks;

import org.springframework.context.ApplicationContext;

/**
 * An in-memory DAO (or DalServices) with state. The Spring context of the unit tests is cached
 * and shared by every test class, so each test starts by resetting all of them
 * ({@link #resetAll(ApplicationContext)}) instead of relying on the previous test to clean up.
 */
public interface ResettableMock {

  /** Restores the initial, empty state. */
  void reset();

  /**
   * Resets every mock of the context.
   *
   * @param context the unit-test context (UnitTestConfig)
   */
  static void resetAll(ApplicationContext context) {
    context.getBeansOfType(ResettableMock.class).values().forEach(ResettableMock::reset);
  }
}
