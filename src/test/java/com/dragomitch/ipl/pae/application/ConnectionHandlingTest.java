package com.dragomitch.ipl.pae.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dragomitch.ipl.pae.domain.exceptions.InvalidCredentialsException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import com.dragomitch.ipl.pae.domain.port.in.SessionService;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A use case releases the connection it took, also when it fails. The read-only use cases that
 * used to open the connection themselves (options, programmes, sign-in, the options of a partner)
 * and {@code PartnerService.showOne} are checked with the real Spring transaction manager on a mocked
 * JDBC connection, over the in-memory DAOs: a failure rolls the transaction back and the
 * connection goes back to the pool.
 */
@SpringJUnitConfig(TransactionBoundaryTest.TransactionalConfig.class)
class ConnectionHandlingTest {

  @Autowired
  private Connection connection;
  @Autowired
  private DataSource dataSource;
  @Autowired
  private OptionService optionService;
  @Autowired
  private ProgrammeService programmeService;
  @Autowired
  private SessionService sessionService;
  @Autowired
  private PartnerService partnerService;

  @BeforeEach
  void forgetPreviousCalls() {
    clearInvocations(connection, dataSource);
  }

  private void assertFailsAndReleasesTheConnection(ThrowingCallable call,
      Class<? extends Throwable> expected) throws SQLException {
    assertThatThrownBy(call).isInstanceOf(expected);

    verify(dataSource, times(1)).getConnection();
    verify(connection).rollback();
    verify(connection, never()).commit();
    verify(connection).close();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  void anUnknownOptionReleasesTheConnection() throws SQLException {
    assertFailsAndReleasesTheConnection(() -> optionService.findAllPartnersByOption("XXX"),
        ResourceNotFoundException.class);
  }

  @Test
  void aSuccessfulReadReleasesTheConnection() throws SQLException {
    assertThat(optionService.showAll()).hasSize(5);
    assertThat(programmeService.showAll()).isNotEmpty();
    assertThat(programmeService.showOne(1).getProgrammeName()).isEqualTo("Erasmus+");

    verify(dataSource, times(3)).getConnection();
    verify(connection, times(3)).commit();
    verify(connection, never()).rollback();
    verify(connection, atLeastOnce()).close();
  }

  @Test
  void aFailedSigninReleasesTheConnection() throws SQLException {
    assertFailsAndReleasesTheConnection(() -> sessionService.signin("nobody", "secret"),
        InvalidCredentialsException.class);
  }

  @Test
  void theOptionsOfAnUnknownPartnerReleaseTheConnection() throws SQLException {
    assertFailsAndReleasesTheConnection(() -> partnerService.findAllPartnerOption(42),
        ResourceNotFoundException.class);
  }

  @Test
  void anUnknownPartnerIsNotFoundAndTheTransactionIsRolledBack() throws SQLException {
    assertFailsAndReleasesTheConnection(() -> partnerService.showOne(42),
        ResourceNotFoundException.class);
  }
}
