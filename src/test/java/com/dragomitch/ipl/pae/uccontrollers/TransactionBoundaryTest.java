package com.dragomitch.ipl.pae.uccontrollers;

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
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The transaction boundaries of the use cases, with the real Spring transaction manager on a
 * mocked JDBC connection: one transaction (one connection) per use-case call, commit on success,
 * rollback when the use case throws, read-only connections for the queries, and the
 * optimistic-locking error of a DAO update rolls back and reaches the caller.
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

  @Autowired
  private Connection connection;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private UserUcc userUcc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private UserDao userDao;

  private UserDto student;

  @BeforeEach
  void setUp() throws SQLException {
    clearInvocations(connection, dataSource);
    reset(userDao);
    student = (UserDto) entityFactory.build(UserDto.class);
    student.setId(5);
    student.setVersion(3);
    student.setRole(UserDto.ROLE_STUDENT);
  }

  @Test
  void aSuccessfulUseCaseCommitsItsTransactionAndReleasesTheConnection() throws SQLException {
    when(userDao.findById(5)).thenReturn(student);

    userUcc.promoteToProfessor(5);

    verify(dataSource, times(1)).getConnection();
    verify(connection).commit();
    verify(connection, never()).rollback();
    verify(connection).close();
    // only the role and the version are written, with the version read in the transaction
    verify(userDao).promoteToProfessor(5, 3);
    verify(userDao, never()).update(any());
  }

  @Test
  void aUseCaseThatThrowsRollsBack() throws SQLException {
    when(userDao.findById(5)).thenReturn(null);

    assertThrows(ResourceNotFoundException.class, () -> userUcc.promoteToProfessor(5));

    verify(connection).rollback();
    verify(connection, never()).commit();
    verify(connection).close();
  }

  @Test
  void aStaleVersionReachesTheCallerAndRollsBack() throws SQLException {
    // the JDBC DAO updates "WHERE user_id = ? AND version = ?" and throws when no row matched
    when(userDao.findById(5)).thenReturn(student);
    doThrow(new ConcurrentModificationException()).when(userDao).promoteToProfessor(5, 3);

    assertThrows(ConcurrentModificationException.class, () -> userUcc.promoteToProfessor(5));

    verify(userDao).promoteToProfessor(5, 3);
    assertEquals(3, student.getVersion(), "the DAO checks the version read in the transaction");
    verify(connection).rollback();
    verify(connection, never()).commit();
  }

  @Test
  void queriesRunInReadOnlyTransactions() throws SQLException {
    when(userDao.findAll()).thenReturn(List.of(student));

    assertEquals(1, userUcc.showAll().size());

    verify(connection).setReadOnly(true);
    verify(connection).commit();
  }
}
