package com.dragomitch.ipl.pae.persistence.implementations;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring context of the DAO integration tests: the <em>real</em> persistence beans (every JDBC
 * DAO, {@code DalBackendServicesImpl}), the real {@code EntityFactory} and a
 * {@code DataSourceTransactionManager}, on top of a throw-away PostgreSQL 16 server started
 * in-process by zonky's embedded-postgres (no Docker needed).
 *
 * <p>The Spring TestContext framework caches this context, so the server is started and
 * {@code SQLRessources/init.sql} is loaded <strong>once per test JVM</strong>; the server is
 * stopped when the context is closed at JVM shutdown. Tests never commit (see
 * {@link AbstractDaoIT}), so the database stays in its initial state between tests.
 *
 * <p>It is a {@link TestConfiguration} so that the {@code @SpringBootTest} component scan of
 * {@code ApplicationTests} ignores it.
 */
@TestConfiguration(proxyBeanMethods = false)
@ComponentScan(basePackages = {
    "com.dragomitch.ipl.pae.persistence.implementations",
    "com.dragomitch.ipl.pae.business.implementations"},
    // do not pick up other test configurations living in the same packages
    excludeFilters = @ComponentScan.Filter(TestConfiguration.class))
public class DaoItConfig {

  /** Schema + reference data of the application (options, programmes, countries, documents). */
  static final Path INIT_SQL = Path.of("SQLRessources", "init.sql");

  @Bean(destroyMethod = "close")
  EmbeddedPostgres embeddedPostgres() throws IOException {
    return EmbeddedPostgres.builder()
        .setServerConfig("timezone", "UTC")
        .setServerConfig("lc_messages", "C")
        .start();
  }

  @Bean(destroyMethod = "close")
  DataSource dataSource(EmbeddedPostgres postgres) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(postgres.getJdbcUrl("postgres", "postgres"));
    config.setUsername("postgres");
    config.setMaximumPoolSize(4);
    config.setPoolName("dao-it");
    HikariDataSource dataSource = new HikariDataSource(config);

    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.addScript(new FileSystemResource(initSql()));
    populator.addScript(new ClassPathResource("db/it-setup.sql"));
    populator.execute(dataSource);
    return dataSource;
  }

  @Bean
  PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  /**
   * Locates init.sql from the Maven {@code basedir} (set by Surefire/Failsafe) or, when the test
   * is launched from an IDE, from the working directory.
   */
  private static Path initSql() {
    Path base = Path.of(System.getProperty("basedir", "."));
    Path script = base.resolve(INIT_SQL);
    if (!Files.isRegularFile(script)) {
      throw new IllegalStateException("Cannot find " + script.toAbsolutePath()
          + ": run the integration tests from the repository root");
    }
    return script;
  }
}
