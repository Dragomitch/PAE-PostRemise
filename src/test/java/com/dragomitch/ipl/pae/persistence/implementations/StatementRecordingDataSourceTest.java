package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

/** The leak detector of {@code DaoErrorHandlingIT} sees what it must see. */
class StatementRecordingDataSourceTest {

  @Test
  void recordsTheConnectionsAndStatementsOnlyWhileRecording() throws Exception {
    DataSource pool = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement prepared = mock(PreparedStatement.class);
    Statement plain = mock(Statement.class);
    when(pool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement("SELECT 1")).thenReturn(prepared);
    when(connection.createStatement()).thenReturn(plain);
    StatementRecordingDataSource dataSource = new StatementRecordingDataSource(pool);

    Connection before = dataSource.getConnection();
    before.createStatement();
    dataSource.startRecording();
    Connection during = dataSource.getConnection();
    before.prepareStatement("SELECT 1");
    during.createStatement();
    dataSource.stopRecording();
    during.createStatement();

    assertThat(dataSource.connections()).containsExactly(during);
    assertThat(dataSource.statements()).containsExactly(prepared, plain);
    // a statement that is never closed is visible as such
    assertThat(dataSource.statements().get(0).isClosed()).isFalse();
  }

  @Test
  void rethrowsTheDriverExceptionAndClosesThePool() throws Exception {
    HikariLikePool pool = mock(HikariLikePool.class);
    Connection connection = mock(Connection.class);
    SQLException failure = new SQLException("boom");
    when(pool.getConnection("u", "p")).thenReturn(connection);
    when(connection.prepareStatement("BAD")).thenThrow(failure);
    StatementRecordingDataSource dataSource = new StatementRecordingDataSource(pool);

    Connection wrapped = dataSource.getConnection("u", "p");
    assertThatThrownBy(() -> wrapped.prepareStatement("BAD")).isSameAs(failure);
    dataSource.close();
    verify(pool).close();
  }

  /** A closeable pool, like Hikari. */
  interface HikariLikePool extends DataSource, AutoCloseable {
  }
}
