package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.DalServices;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.stereotype.Component;

/**
 * Thread-bound JDBC connection handling on top of the pooled {@link DataSource} configured by
 * Spring Boot ({@code spring.datasource.*}).
 */
@Component
class DalServicesImpl implements DalServices, DalBackendServices {

  private final DataSource connectionPool;
  private final ThreadLocal<Connection> threadMap;
  private final ThreadLocal<Integer> semaphore;

  public DalServicesImpl(DataSource connectionPool) {
    this.connectionPool = connectionPool;
    this.threadMap = new ThreadLocal<Connection>();
    this.semaphore = new ThreadLocal<Integer>();
  }

  @Override
  public PreparedStatement prepareStatement(String sql) {
    try {
      return threadMap.get().prepareStatement(sql);
    } catch (SQLException ex) {
      throw new FatalException("Impossible to prepare an SQL statement.", ex);
    }
  }

  @Override
  public void startTransaction() {
    try {
      threadMap.get().setAutoCommit(false);
    } catch (SQLException ex) {
      throw new FatalException("Impossible to start an SQL transaction.", ex);
    }
  }

  @Override
  public void commit() {
    try {
      threadMap.get().commit();
      threadMap.get().setAutoCommit(true);
    } catch (SQLException ex) {
      throw new FatalException("Impossible to commit an SQL transaction.", ex);
    }
  }

  @Override
  public void rollback() {
    try {
      threadMap.get().rollback();
      threadMap.get().setAutoCommit(true);
    } catch (SQLException ex) {
      throw new FatalException("Impossible to rollback an SQL transaction.", ex);
    }
  }

  @Override
  public void openConnection() {
    Integer sema;
    if (semaphore.get() == null) {
      sema = 0;
    } else {
      sema = semaphore.get();
    }
    if (sema == 0) {
      Connection connection = null;
      try {
        connection = connectionPool.getConnection();
      } catch (SQLException ex) {
        throw new FatalException("Impossible to get an SQL connection.", ex);
      }
      threadMap.set(connection);
    }
    semaphore.set(++sema);
  }

  @Override
  public void closeConnection() {
    Integer sema = semaphore.get();
    if (sema == 1) {
      Connection connection = null;
      connection = threadMap.get();
      threadMap.remove();
      try {
        connection.close();
      } catch (SQLException ex) {
        throw new FatalException("Impossible to close an SQL connection.", ex);
      }
    }
    semaphore.set(--sema);
  }

  /*
   * nareux, adjectif : Se dit d'une personne qui est dégoûtée par le contact avec la saleté, en
   * particulier au niveau alimentaire.
   */

}
