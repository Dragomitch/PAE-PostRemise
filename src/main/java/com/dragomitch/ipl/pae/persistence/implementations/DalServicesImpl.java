package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.DalServices;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
class DalServicesImpl implements DalServices, DalBackendServices {

  private final HikariDataSource connectionPool;
  private ThreadLocal<Connection> threadMap;
  private ThreadLocal<Integer> semaphore;

  public DalServicesImpl(@Value("${host}") String dbHost,
                         @Value("${port}") String dbPort,
                         @Value("${database}") String dbDatabase,
                         @Value("${username}") String dbUser,
                         @Value("${password}") String dbPassword,
                         @Value("${driver_class}") String drivers) {
    threadMap = new ThreadLocal<>();
    semaphore = new ThreadLocal<>();
    connectionPool = new HikariDataSource();
    String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbDatabase + "?autoReconnect=true";
    connectionPool.setJdbcUrl(url);
    connectionPool.setUsername(dbUser);
    connectionPool.setPassword(dbPassword);
    connectionPool.setDriverClassName(drivers);
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
