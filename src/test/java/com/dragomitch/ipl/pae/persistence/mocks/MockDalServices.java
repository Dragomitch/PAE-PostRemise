package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.persistence.DalServices;

/**
 * Connection handling of the unit tests: nothing to open, but the balance of
 * {@code openConnection}/{@code closeConnection} and of the transactions is tracked so that the
 * tests can check that a use case always releases what it took, also when it fails.
 */
public class MockDalServices implements DalServices, ResettableMock {

  private int openConnections;
  private int openTransactions;
  private int commits;
  private int rollbacks;

  @Override
  public void startTransaction() {
    openTransactions++;
  }

  @Override
  public void commit() {
    openTransactions--;
    commits++;
  }

  @Override
  public void rollback() {
    openTransactions--;
    rollbacks++;
  }

  @Override
  public void openConnection() {
    openConnections++;
  }

  @Override
  public void closeConnection() {
    openConnections--;
  }

  /** Connections opened and not closed yet. */
  public int getOpenConnections() {
    return openConnections;
  }

  /** Transactions started and neither committed nor rolled back yet. */
  public int getOpenTransactions() {
    return openTransactions;
  }

  public int getCommits() {
    return commits;
  }

  public int getRollbacks() {
    return rollbacks;
  }

  /** Forgets everything (a previous test may have left an unbalanced state on purpose). */
  @Override
  public void reset() {
    openConnections = 0;
    openTransactions = 0;
    commits = 0;
    rollbacks = 0;
  }
}
