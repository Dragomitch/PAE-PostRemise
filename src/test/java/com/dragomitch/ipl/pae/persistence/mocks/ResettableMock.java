package com.dragomitch.ipl.pae.persistence.mocks;

/**
 * An in-memory mock DAO holding data written by the tests. The Spring test context (and so each
 * mock) is shared by every test class using {@code UnitTestConfig}: {@link MockDaoResetListener}
 * empties them before each test so that no test depends on what another one left behind,
 * whatever the execution order.
 */
public interface ResettableMock {

  /** Removes every entity stored by the tests. */
  void empty();
}
