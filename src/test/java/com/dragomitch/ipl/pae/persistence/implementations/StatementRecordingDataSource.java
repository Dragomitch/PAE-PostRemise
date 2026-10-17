package com.dragomitch.ipl.pae.persistence.implementations;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * The {@link DataSource} of the DAO integration tests: it delegates to the pool and, while
 * {@linkplain #startRecording() recording}, remembers every connection it hands out and every
 * statement ({@link Statement}, {@link PreparedStatement}, {@link CallableStatement}) created on
 * any of its connections. The Spring Data repositories, {@code JdbcClient} and the test helpers
 * all go through it, since the transaction binds the (wrapped) connection it got from here.
 *
 * <p>{@code DaoErrorHandlingIT} uses it to check that no DAO method leaks a statement or takes a
 * connection of its own outside the caller's transaction.
 */
public final class StatementRecordingDataSource extends DelegatingDataSource
    implements AutoCloseable {

  private final List<Statement> statements = new CopyOnWriteArrayList<>();
  private final List<Connection> connections = new CopyOnWriteArrayList<>();
  private volatile boolean recording;

  public StatementRecordingDataSource(DataSource pool) {
    super(pool);
  }

  /** Forgets what was recorded so far and records from now on. */
  public void startRecording() {
    statements.clear();
    connections.clear();
    recording = true;
  }

  /** Stops recording; what was recorded stays available until the next start. */
  public void stopRecording() {
    recording = false;
  }

  /**
   * The statements created while recording.
   *
   * @return the statements, in creation order
   */
  public List<Statement> statements() {
    return List.copyOf(statements);
  }

  /**
   * The connections handed out by this data source while recording.
   *
   * @return the connections, in order
   */
  public List<Connection> connections() {
    return List.copyOf(connections);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return wrap(obtainTargetDataSource().getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return wrap(obtainTargetDataSource().getConnection(username, password));
  }

  /** Closes the pool when the test context shuts down. */
  @Override
  public void close() throws Exception {
    if (obtainTargetDataSource() instanceof AutoCloseable pool) {
      pool.close();
    }
  }

  private Connection wrap(Connection connection) {
    Connection wrapped = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[] {Connection.class}, new ConnectionHandler(connection));
    if (recording) {
      connections.add(wrapped);
    }
    return wrapped;
  }

  /** Records the statements created on a connection. */
  private final class ConnectionHandler implements InvocationHandler {
    private final Connection target;

    private ConnectionHandler(Connection target) {
      this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // identity of the proxy itself (the target would compare itself with the proxy)
      if (method.getName().equals("equals") && method.getParameterCount() == 1) {
        return proxy == args[0];
      }
      if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
        return System.identityHashCode(proxy);
      }
      Object result;
      try {
        result = method.invoke(target, args);
      } catch (InvocationTargetException ex) {
        throw ex.getCause();
      }
      if (recording && result instanceof Statement statement) {
        statements.add(statement);
      }
      return result;
    }
  }
}
