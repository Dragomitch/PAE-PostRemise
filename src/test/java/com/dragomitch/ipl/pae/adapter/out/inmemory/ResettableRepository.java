package com.dragomitch.ipl.pae.adapter.out.inmemory;

/**
 * An in-memory mock DAO holding data written by the tests. The Spring test context (and so each
 * mock) is shared by every test class using {@code UnitTestConfig}: {@link
 * InMemoryRepositoryResetListener} empties them before each test so that no test depends on what
 * another one left behind, whatever the execution order.
 */
public interface ResettableRepository {

  /** Removes every entity stored by the tests. */
  void empty();
}
