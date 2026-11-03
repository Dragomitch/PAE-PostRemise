package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * The Flyway migrations of {@code src/main/resources/db/migration} against {@code
 * SQLRessources/init.sql}, on an embedded PostgreSQL (ADR 0003).
 *
 * <p>Flyway runs as in the application: Spring Boot's {@link FlywayAutoConfiguration} with the
 * {@code spring.flyway.*} properties of {@code application.properties}, so the {@link Flyway} bean
 * migrates the database when the context starts. Every test works on a new database of the same
 * server.
 *
 * <ul>
 *   <li>{@code V1__baseline.sql} on an empty database and {@code init.sql} on another produce the
 *       same schema (columns, constraints, indexes, sequences) and the same rows.
 *   <li>A database created from {@code init.sql} before Flyway is adopted as version 1 ({@code
 *       baseline-on-migrate}): no migration runs and its data is left as it was.
 *   <li>A second start on a migrated database has nothing to do.
 * </ul>
 */
class FlywayBaselineIT {

  private static final String SCHEMA = RepositoryItConfig.SCHEMA;

  private static final Path INIT_SQL = Path.of("SQLRessources", "init.sql");

  private static final String HISTORY_TABLE = "flyway_schema_history";

  /** Columns of every table: name, position, type, length, nullability, default. */
  private static final String COLUMNS =
      "SELECT table_name, column_name, ordinal_position, data_type, character_maximum_length,"
          + " numeric_precision, is_nullable, column_default FROM information_schema.columns"
          + " WHERE table_schema = ? AND table_name <> '"
          + HISTORY_TABLE
          + "' ORDER BY table_name, ordinal_position";

  /** Constraints by table, name and type (primary keys, foreign keys, not null). */
  private static final String CONSTRAINTS =
      "SELECT table_name, constraint_name, constraint_type FROM information_schema.table_constraints"
          + " WHERE constraint_schema = ? AND table_name <> '"
          + HISTORY_TABLE
          + "' ORDER BY table_name, constraint_name";

  /** What each constraint says: the referenced table and columns of a foreign key, etc. */
  private static final String CONSTRAINT_DEFINITIONS =
      "SELECT t.relname, c.conname, pg_get_constraintdef(c.oid) FROM pg_constraint c"
          + " JOIN pg_class t ON t.oid = c.conrelid JOIN pg_namespace n ON n.oid = c.connamespace"
          + " WHERE n.nspname = ? AND t.relname <> '"
          + HISTORY_TABLE
          + "' ORDER BY t.relname, c.conname";

  private static final String INDEXES =
      "SELECT tablename, indexname, indexdef FROM pg_indexes WHERE schemaname = ? AND tablename <> '"
          + HISTORY_TABLE
          + "' ORDER BY tablename, indexname";

  /** The sequences of the SERIAL columns, with the value the reference data left them at. */
  private static final String SEQUENCES =
      "SELECT sequencename, data_type, start_value, increment_by, last_value FROM pg_sequences"
          + " WHERE schemaname = ? ORDER BY sequencename";

  private static final String TABLES =
      "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name <> '"
          + HISTORY_TABLE
          + "' ORDER BY table_name";

  private static final String HISTORY =
      "SELECT version, description, type, success FROM "
          + SCHEMA
          + "."
          + HISTORY_TABLE
          + " WHERE type <> 'SCHEMA' ORDER BY installed_rank";

  /** The application's Flyway: Spring Boot's auto-configuration and application.properties. */
  private static final ApplicationContextRunner APPLICATION_FLYWAY =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class));

  private static EmbeddedPostgres postgres;

  @BeforeAll
  static void startPostgres() throws IOException {
    postgres = EmbeddedPostgres.builder().setServerConfig("timezone", "UTC").start();
  }

  @AfterAll
  static void stopPostgres() throws IOException {
    postgres.close();
  }

  @Test
  void theBaselineMigrationCreatesTheSchemaOfInitSql() throws SQLException {
    DataSource migrated = newDatabase("migrated");
    // only V1: init.sql is the schema at version 1 and stays so when later migrations come
    startApplicationFlyway(
        migrated,
        flyway -> assertEquals("1", flyway.info().current().getVersion().getVersion()),
        "spring.flyway.target=1");
    DataSource loaded = newDatabase("loaded_from_init_sql");
    loadInitSql(loaded);

    assertEquals(List.of("1 | baseline | SQL | true"), rows(migrated, HISTORY));
    for (String query : List.of(COLUMNS, CONSTRAINTS, CONSTRAINT_DEFINITIONS, INDEXES, SEQUENCES)) {
      List<String> expected = rows(loaded, query, SCHEMA);
      assertFalse(expected.isEmpty(), query);
      assertEquals(expected, rows(migrated, query, SCHEMA), query);
    }
    List<String> tables = rows(loaded, TABLES, SCHEMA);
    assertEquals(tables, rows(migrated, TABLES, SCHEMA));
    for (String table : tables) {
      String allRows = "SELECT t::text FROM " + SCHEMA + ".\"" + table + "\" t ORDER BY 1";
      assertEquals(rows(loaded, allRows), rows(migrated, allRows), table);
    }
  }

  @Test
  void aDatabaseCreatedFromInitSqlIsAdoptedAsVersion1WithoutBeingTouched() throws SQLException {
    DataSource database = newDatabase("adopted");
    loadInitSql(database);
    JdbcTemplate jdbc = new JdbcTemplate(database);
    // a row only this database has, as a database in use would have
    jdbc.update(
        "INSERT INTO " + SCHEMA + ".denial_reasons (reason) VALUES ('Kept by the baseline')");
    String counts =
        "SELECT (SELECT count(*) FROM "
            + SCHEMA
            + ".denial_reasons) || ' / ' || (SELECT count(*) FROM "
            + SCHEMA
            + ".countries)";
    List<String> before = rows(database, counts);

    startApplicationFlyway(
        database,
        flyway -> {
          assertEquals("1", flyway.info().current().getVersion().getVersion());
          assertEquals(0, flyway.info().pending().length);
        });

    assertEquals(List.of("1 | << Flyway Baseline >> | BASELINE | true"), rows(database, HISTORY));
    assertEquals(before, rows(database, counts));
    assertEquals(List.of("2 / 250"), before);
  }

  @Test
  void aSecondStartHasNothingToMigrate() throws SQLException {
    DataSource database = newDatabase("restarted");
    startApplicationFlyway(database, flyway -> {});
    List<String> history = rows(database, HISTORY);

    startApplicationFlyway(database, flyway -> assertEquals(0, flyway.info().pending().length));

    assertEquals(history, rows(database, HISTORY));
  }

  /**
   * Starts a context with the application's Flyway on the database: the {@link Flyway} bean
   * migrates it at start-up, as in the application.
   */
  private static void startApplicationFlyway(
      DataSource database, Consumer<Flyway> assertions, String... properties) {
    APPLICATION_FLYWAY
        .withBean(DataSource.class, () -> database)
        .withPropertyValues(properties)
        .run(context -> {
          assertNull(context.getStartupFailure());
          assertions.accept(context.getBean(Flyway.class));
        });
  }

  /** A new, empty database of the embedded server. */
  private static DataSource newDatabase(String name) throws SQLException {
    try (Connection connection = postgres.getPostgresDatabase().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE DATABASE " + name);
    }
    return postgres.getDatabase("postgres", name);
  }

  private static void loadInitSql(DataSource database) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    populator.addScript(new FileSystemResource(initSql()));
    populator.execute(database);
  }

  /** The rows of a query, each one as its columns joined with {@code " | "}. */
  private static List<String> rows(DataSource database, String sql, Object... arguments) {
    return new JdbcTemplate(database)
        .query(
            sql,
            (resultSet, rowNumber) -> {
              List<String> values = new ArrayList<>();
              for (int column = 1; column <= resultSet.getMetaData().getColumnCount(); column++) {
                values.add(String.valueOf(resultSet.getObject(column)));
              }
              return String.join(" | ", values);
            },
            arguments);
  }

  /**
   * Locates init.sql from the Maven {@code basedir} (set by Failsafe) or, when the test is launched
   * from an IDE, from the working directory.
   */
  private static Path initSql() {
    Path script = Path.of(System.getProperty("basedir", ".")).resolve(INIT_SQL);
    if (!Files.isRegularFile(script)) {
      throw new IllegalStateException(
          "Cannot find "
              + script.toAbsolutePath()
              + ": run the integration tests from the repository root");
    }
    return script;
  }
}
