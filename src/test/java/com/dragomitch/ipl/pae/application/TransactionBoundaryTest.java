package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The transaction boundaries of the use cases, with the real Spring transaction manager on a mocked
 * JDBC connection: one transaction (one connection) per use-case call, commit on success, rollback
 * when the use case throws, read-only connections for the queries, and the optimistic-locking error
 * of a DAO update rolls back and reaches the caller.
 *
 * <p>The application runs a {@code JpaTransactionManager}; a {@code DataSourceTransactionManager}
 * is enough here, where the DAOs are mocks: the boundaries are the {@code @Transactional} use-case
 * proxies', whatever the manager.
 */
@SpringJUnitConfig(TransactionBoundaryTest.TransactionalConfig.class)
class TransactionBoundaryTest {

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  @Import(UnitTestConfig.class)
  static class TransactionalConfig {

    @Bean
    Connection connection() {
      return mock(Connection.class);
    }

    @Bean
    DataSource dataSource(Connection connection) throws SQLException {
      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenAnswer(invocation -> connection);
      return dataSource;
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }

  @Autowired private Connection connection;

  @Autowired private DataSource dataSource;

  @Autowired private UserService userService;

  @Autowired private EntityFactory entityFactory;

  @MockitoBean private UserRepository userRepository;

  private UserDto student;

  @BeforeEach
  void setUp() throws SQLException {
    clearInvocations(connection, dataSource);
    reset(userRepository);
    student = (UserDto) entityFactory.build(UserDto.class);
    student.setId(5);
    student.setVersion(3);
    student.setRole(UserDto.ROLE_STUDENT);
  }

  @Test
  void aSuccessfulUseCaseCommitsItsTransactionAndReleasesTheConnection() throws SQLException {
    when(userRepository.findById(5)).thenReturn(student);

    userService.promoteToProfessor(5);

    verify(dataSource, times(1)).getConnection();
    verify(connection).commit();
    verify(connection, never()).rollback();
    verify(connection).close();
    // only the role and the version are written, with the version read in the transaction
    verify(userRepository).promoteToProfessor(5, 3);
    verify(userRepository, never()).update(any());
  }

  @Test
  void aUseCaseThatThrowsRollsBack() throws SQLException {
    when(userRepository.findById(5)).thenReturn(null);

    assertThrows(ResourceNotFoundException.class, () -> userService.promoteToProfessor(5));

    verify(connection).rollback();
    verify(connection, never()).commit();
    verify(connection).close();
  }

  @Test
  void aStaleVersionReachesTheCallerAndRollsBack() throws SQLException {
    // the JDBC DAO updates "WHERE user_id = ? AND version = ?" and throws when no row matched
    when(userRepository.findById(5)).thenReturn(student);
    doThrow(new ConcurrentModificationException()).when(userRepository).promoteToProfessor(5, 3);

    assertThrows(ConcurrentModificationException.class, () -> userService.promoteToProfessor(5));

    verify(userRepository).promoteToProfessor(5, 3);
    assertEquals(3, student.getVersion(), "the DAO checks the version read in the transaction");
    verify(connection).rollback();
    verify(connection, never()).commit();
  }

  @Test
  void queriesRunInReadOnlyTransactions() throws SQLException {
    when(userRepository.findAll()).thenReturn(List.of(student));

    assertEquals(1, userService.showAll().size());

    verify(connection).setReadOnly(true);
    verify(connection).commit();
  }
}
