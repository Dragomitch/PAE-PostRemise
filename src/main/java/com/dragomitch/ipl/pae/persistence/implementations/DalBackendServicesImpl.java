package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Hands the DAOs statements prepared on the JDBC connection of the current Spring transaction.
 *
 * <p>The connection is bound to the thread by the {@code DataSourceTransactionManager} that
 * Spring Boot configures on top of the pooled {@link DataSource} ({@code spring.datasource.*}):
 * transactions are opened, committed, rolled back and the connection returned to the pool by
 * {@code @Transactional} on the use-case services, never here. A DAO call outside a transaction
 * is a programming error (the connection would never be released), so it is rejected.
 */
@Component
class DalBackendServicesImpl implements DalBackendServices {

  private final DataSource dataSource;

  DalBackendServicesImpl(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public PreparedStatement prepareStatement(String sql) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException(
          "DAO called outside a transaction: annotate the calling service with @Transactional");
    }
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      return connection.prepareStatement(sql);
    } catch (SQLException ex) {
      throw new FatalException("Impossible to prepare an SQL statement.", ex);
    }
  }

}
