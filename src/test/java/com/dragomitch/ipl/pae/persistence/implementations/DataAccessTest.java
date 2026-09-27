package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The Spring Data DAOs keep the exception contract of the DAO interfaces. */
class DataAccessTest {

  private TransactionTemplate transaction;

  @BeforeEach
  void setUp() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(mock(Connection.class));
    transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  private <T> T callInTransaction(Supplier<T> work) {
    return transaction.execute(status -> DataAccess.call(work));
  }

  @Test
  void returnsTheResultOfTheWork() {
    assertThat(callInTransaction(() -> "ok")).isEqualTo("ok");
  }

  @Test
  void refusesToRunOutsideATransaction() {
    assertThatThrownBy(() -> DataAccess.call(() -> "never"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aStaleVersionIsAConcurrentModification() {
    OptimisticLockingFailureException stale = new OptimisticLockingFailureException("stale");

    assertThatThrownBy(() -> callInTransaction(() -> {
      throw stale;
    })).isInstanceOf(ConcurrentModificationException.class).hasCause(stale);
  }

  @Test
  void aDataAccessExceptionIsFatal() {
    DataIntegrityViolationException violation = new DataIntegrityViolationException("fk");

    assertThatThrownBy(() -> callInTransaction(() -> {
      throw violation;
    })).isInstanceOf(FatalException.class).hasCause(violation);
  }

  @Test
  void aFailedInsertOrUpdateOfSpringDataIsFatal() {
    DbActionExecutionException failure =
        new DbActionExecutionException(null, new IllegalStateException("boom"));

    assertThatThrownBy(() -> callInTransaction(() -> {
      throw failure;
    })).isInstanceOf(FatalException.class).hasCause(failure);
  }

  @Test
  void otherExceptionsAreNotTranslated() {
    assertThatThrownBy(() -> callInTransaction(() -> {
      throw new IllegalArgumentException("bug");
    })).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aNullIdGivesANullReference() {
    assertThat(DataAccess.<Object, String>reference(null)).isNull();
    assertThat(DataAccess.<Object, String>reference("BE").getId()).isEqualTo("BE");
  }
}
