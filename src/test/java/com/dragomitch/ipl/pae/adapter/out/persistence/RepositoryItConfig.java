package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring context of the DAO integration tests: the <em>real</em> persistence beans (the JPA
 * persistence adapters with their entities and Spring Data repositories, and the remaining JDBC
 * DAOs), an {@code EntityManagerFactory} that validates the entity mappings against the schema
 * ({@code ddl-auto=validate}) and a {@code JpaTransactionManager}, on top of a throw-away
 * PostgreSQL server started in-process by zonky's embedded-postgres (no Docker needed). The schema
 * and its reference data come from the application's Flyway migrations ({@code
 * src/main/resources/db/migration}), as in production.
 *
 * <p>The Spring TestContext framework caches this context, so the server is started and migrated
 * <strong>once per test JVM</strong>; the server is stopped when the context is closed at JVM
 * shutdown. Tests never commit (see {@link AbstractRepositoryIT}), so the database stays in its
 * initial state between tests.
 *
 * <p>It is a {@link TestConfiguration} so that the {@code @SpringBootTest} component scan of {@code
 * ApplicationTests} ignores it.
 */
@TestConfiguration(proxyBeanMethods = false)
@ComponentScan(
    basePackages = "com.dragomitch.ipl.pae.adapter.out.persistence",
    // do not pick up other test configurations living in the same packages
    excludeFilters = @ComponentScan.Filter(TestConfiguration.class))
public class RepositoryItConfig {

  static final String PERSISTENCE_PACKAGE = "com.dragomitch.ipl.pae.adapter.out.persistence";

  /**
   * The schema of the application ({@code hibernate.default_schema}, {@code
   * spring.flyway.schemas}).
   */
  static final String SCHEMA = "student_exchange_tools";

  /** Where the migrations live ({@code spring.flyway.locations}). */
  static final String MIGRATIONS = "classpath:db/migration";

  /**
   * The system clock, as in the application ({@code ClockConfig}): the adapters take the current
   * year from it, and the ITs express their fixtures relative to the system's current year.
   */
  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

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

    // the schema and the reference data, as the application gets them at start-up
    Flyway.configure()
        .dataSource(dataSource)
        .locations(MIGRATIONS)
        .schemas(SCHEMA)
        .load()
        .migrate();
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.addScript(new ClassPathResource("db/it-setup.sql"));
    populator.execute(dataSource);
    return dataSource;
  }

  /**
   * The JPA setup of {@code application.properties}, plus {@code ddl-auto=validate}: Hibernate
   * checks every entity mapping against the tables created by the migrations at start-up.
   */
  @Bean
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(dataSource);
    factory.setPackagesToScan(PERSISTENCE_PACKAGE);
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto", "validate",
            "hibernate.default_schema", SCHEMA,
            // read by QueryCountIT
            "hibernate.generate_statistics", "true"));
    return factory;
  }

  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }

  /**
   * Translates the JPA exceptions of the {@code @Repository} persistence adapters into Spring's
   * {@code DataAccessException}s, as Spring Boot does in the application.
   */
  @Bean
  static PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslation() {
    return new PersistenceExceptionTranslationPostProcessor();
  }
}
