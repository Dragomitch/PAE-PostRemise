package com.dragomitch.ipl.pae.persistence.jdbc;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Spring Data JDBC setup of the persistence layer.
 *
 * <p>The aggregates ({@code entity}) map the hand-written schema of {@code SQLRessources/init.sql}
 * explicitly ({@code @Table(schema = SCHEMA, name = ...)}, {@code @Column} where the column name
 * is not the snake_case of the property). The repositories ({@code repository}) run on the
 * {@code NamedParameterJdbcTemplate} of the application, which takes its connection from
 * {@code DataSourceUtils}: repositories and the remaining hand-written JDBC DAOs therefore share
 * the connection and the transaction opened by {@code @Transactional} on the use cases.
 *
 * <p>Declaring this {@link AbstractJdbcConfiguration} makes Spring Boot's own Spring Data JDBC
 * configuration back off, so the application and the DAO integration tests (which have no Boot
 * auto-configuration) use exactly the same setup.
 */
@Configuration(proxyBeanMethods = false)
@EnableJdbcRepositories(basePackages = "com.dragomitch.ipl.pae.persistence.jdbc.repository")
public class JdbcPersistenceConfig extends AbstractJdbcConfiguration {

  /** The PostgreSQL schema holding every table of the application. */
  public static final String SCHEMA = "student_exchange_tools";

  /**
   * The database is always PostgreSQL. Declaring the dialect avoids the default detection, which
   * opens a connection while the context starts (the application must start without a database,
   * see {@code ApplicationTests}).
   */
  @Override
  public Dialect jdbcDialect(NamedParameterJdbcOperations operations) {
    return JdbcPostgresDialect.INSTANCE;
  }
}
