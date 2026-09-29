package com.dragomitch.ipl.pae.persistence.implementations;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The DAOs get their statements from the connection of the current Spring transaction, and only
 * inside one.
 */
class DalBackendServicesImplTest {

  private Connection connection;
  private DataSource dataSource;
  private DalBackendServicesImpl dalBackendServices;
  private TransactionTemplate transaction;

  @BeforeEach
  void setUp() throws SQLException {
    connection = mock(Connection.class);
    dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(connection);
    dalBackendServices = new DalBackendServicesImpl(dataSource);
    transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Test
  void statementsArePreparedOnTheTransactionConnection() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(connection.prepareStatement("SELECT 1")).thenReturn(statement);

    PreparedStatement prepared = transaction.execute(status -> {
      PreparedStatement first = dalBackendServices.prepareStatement("SELECT 1");
      dalBackendServices.prepareStatement("SELECT 1");
      return first;
    });

    assertSame(statement, prepared);
    // one connection for the whole transaction, returned to the pool at its end
    verify(dataSource).getConnection();
    verify(connection).commit();
    verify(connection).close();
  }

  @Test
  void aDaoCallOutsideATransactionIsRefused() throws SQLException {
    assertThrows(IllegalStateException.class,
        () -> dalBackendServices.prepareStatement("SELECT 1"));
    verify(dataSource, never()).getConnection();
  }

  @Test
  void anSqlErrorIsFatal() throws SQLException {
    when(connection.prepareStatement("BAD")).thenThrow(new SQLException("syntax error"));

    assertThrows(FatalException.class, () -> transaction.executeWithoutResult(
        status -> dalBackendServices.prepareStatement("BAD")));
    verify(connection).rollback();
  }
}
