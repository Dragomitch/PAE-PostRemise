package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Base class of every DAO integration test ({@code *IT}, run by maven-failsafe-plugin against a
 * real PostgreSQL, see {@link DaoItConfig}).
 *
 * <h2>Single point of transaction handling</h2>
 * This class is the <strong>only</strong> place of the test suite that knows how connections and
 * transactions are obtained. Tests wrap every DAO call in {@link #inTransaction(Supplier)} or
 * {@link #runInTransaction(Runnable)}, which
 * <ol>
 *   <li>opens a connection and starts a transaction,</li>
 *   <li>loads the {@linkplain #fixtures() fixtures} of the test class in that transaction,</li>
 *   <li>runs the work, and</li>
 *   <li><strong>always rolls back</strong>, whether the work succeeded or threw.</li>
 * </ol>
 * Nothing is ever committed, so tests are isolated from each other and can run in any order.
 *
 * <p>It uses Spring's transaction management, like the application: the work runs in a
 * {@link TransactionTemplate} (the {@code DataSourceTransactionManager} of {@link DaoItConfig})
 * whose status is set to rollback-only, and the fixtures and the helper queries use the
 * transaction-bound connection ({@code DataSourceUtils.getConnection(dataSource)}), the one the
 * Spring Data repositories and the {@code JdbcClient} of the DAOs use. The test classes only rely
 * on the helpers below and on the DAO interfaces: they describe the behaviour every DAO
 * implementation must keep, whatever the persistence technology behind it.
 *
 * <h2>Fixtures</h2>
 * Subclasses list classpath SQL scripts in {@link #fixtures()} (see {@code src/test/resources/db/
 * fixtures}). They use explicit ids below 100000; generated ids start at 100000 (see
 * {@code db/it-setup.sql}).
 */
@SpringJUnitConfig(DaoItConfig.class)
public abstract class AbstractDaoIT {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  protected EntityFactory entityFactory;

  /**
   * Classpath locations of the SQL scripts loaded at the start of every transaction, in order.
   *
   * @return the fixture scripts of the test class (none by default)
   */
  protected List<String> fixtures() {
    return List.of();
  }

  /**
   * Runs {@code work} in a fresh transaction (with the fixtures loaded) that is always rolled back.
   *
   * @param work the code to run, typically DAO calls and assertions
   * @param <T> the type of the result
   * @return what {@code work} returned (DTOs stay usable after the rollback)
   */
  protected <T> T inTransaction(Supplier<T> work) {
    return new TransactionTemplate(transactionManager).execute(status -> {
      // rolled back at the end, even when the work succeeds
      status.setRollbackOnly();
      for (String fixture : fixtures()) {
        withConnection(connection -> {
          ScriptUtils.executeSqlScript(connection,
              new EncodedResource(new ClassPathResource(fixture), StandardCharsets.UTF_8));
          return null;
        });
      }
      return work.get();
    });
  }

  /**
   * Same as {@link #inTransaction(Supplier)} for work that returns nothing.
   *
   * @param work the code to run
   */
  protected void runInTransaction(Runnable work) {
    inTransaction(() -> {
      work.run();
      return null;
    });
  }

  /**
   * Executes an update statement in the current transaction; only valid inside
   * {@link #inTransaction(Supplier)}.
   *
   * @param sql the SQL, with {@code ?} placeholders
   * @param params the parameters
   * @return the number of rows affected
   */
  protected int execute(String sql, Object... params) {
    return withConnection(connection -> {
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        bind(stmt, params);
        return stmt.executeUpdate();
      }
    });
  }

  /**
   * Runs a query in the current transaction and returns every row as a column-name map; only
   * valid inside {@link #inTransaction(Supplier)}. Used to check what a DAO really wrote.
   *
   * @param sql the SQL, with {@code ?} placeholders
   * @param params the parameters
   * @return the rows, in the order returned by the database
   */
  protected List<Map<String, Object>> query(String sql, Object... params) {
    return withConnection(connection -> {
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        bind(stmt, params);
        try (ResultSet rs = stmt.executeQuery()) {
          ResultSetMetaData meta = rs.getMetaData();
          List<Map<String, Object>> rows = new ArrayList<>();
          while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
              row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
          }
          return rows;
        }
      }
    });
  }

  /**
   * Returns the single row of a query; fails if there is not exactly one.
   *
   * @param sql the SQL, with {@code ?} placeholders
   * @param params the parameters
   * @return the row as a column-name map
   */
  protected Map<String, Object> queryForRow(String sql, Object... params) {
    List<Map<String, Object>> rows = query(sql, params);
    if (rows.size() != 1) {
      throw new AssertionError("Expected exactly one row but got " + rows.size() + ": " + sql);
    }
    return rows.get(0);
  }

  /**
   * Builds an entity with the real factory, typed.
   *
   * @param type the DTO or business interface
   * @param <T> the interface type
   * @return a new instance
   */
  protected <T> T build(Class<T> type) {
    return type.cast(entityFactory.build(type));
  }

  @FunctionalInterface
  private interface ConnectionCallback<T> {
    T doWith(Connection connection) throws SQLException;
  }

  /** Gives access to the connection bound to the current transaction. */
  private <T> T withConnection(ConnectionCallback<T> callback) {
    try {
      return callback.doWith(DataSourceUtils.getConnection(dataSource));
    } catch (SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void bind(PreparedStatement stmt, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
  }
}
