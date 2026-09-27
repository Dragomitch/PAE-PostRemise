package com.dragomitch.ipl.pae.resources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Guards against countries being shipped without a flag picture (issue #21).
 *
 * <p>Every country code seeded in {@code SQLRessources/init.sql} must have a matching
 * {@code src/main/webapp/images/flags/<code>.png}, which the front end loads by code.
 */
public class CountryFlagsTest {

  private static final String INIT_SQL = "SQLRessources/init.sql";
  private static final String FLAGS_DIR = "src/main/webapp/images/flags";

  private static final Pattern COUNTRIES_INSERT = Pattern.compile(
      "INSERT\\s+INTO\\s+student_exchange_tools\\.countries\\s+VALUES(.*?);",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern COUNTRY_CODE = Pattern.compile("\\(\\s*'([A-Za-z]{2})'");

  @Test
  public void everySeededCountryHasAFlag() throws IOException {
    Path baseDir = findProjectBaseDir();
    Set<String> codes = readCountryCodes(baseDir.resolve(INIT_SQL));

    // Sanity check so a parsing regression cannot make this test pass vacuously.
    assertTrue(codes.size() > 200,
        "Expected to parse the full country list from " + INIT_SQL + ", got " + codes.size());

    Path flagsDir = baseDir.resolve(FLAGS_DIR);
    List<String> missing = new ArrayList<>();
    for (String code : codes) {
      if (!Files.isRegularFile(flagsDir.resolve(code + ".png"))) {
        missing.add(code);
      }
    }
    assertTrue(missing.isEmpty(),
        "Countries without a flag in " + FLAGS_DIR + ": " + missing);
  }

  private static Set<String> readCountryCodes(Path initSql) throws IOException {
    String sql = new String(Files.readAllBytes(initSql), StandardCharsets.UTF_8);
    Set<String> codes = new LinkedHashSet<>();
    Matcher insert = COUNTRIES_INSERT.matcher(sql);
    while (insert.find()) {
      Matcher code = COUNTRY_CODE.matcher(insert.group(1));
      while (code.find()) {
        codes.add(code.group(1).toUpperCase());
      }
    }
    return codes;
  }

  /**
   * Resolves the Maven project base dir: Surefire's {@code basedir} property when set,
   * otherwise the first ancestor of the working directory that contains the seed script.
   */
  private static Path findProjectBaseDir() {
    List<Path> candidates = new ArrayList<>();
    String basedir = System.getProperty("basedir");
    if (basedir != null && !basedir.isEmpty()) {
      candidates.add(Paths.get(basedir));
    }
    for (Path dir = Paths.get("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
      candidates.add(dir);
    }
    for (Path dir : candidates) {
      if (Files.isRegularFile(dir.resolve(INIT_SQL)) && Files.isDirectory(dir.resolve(FLAGS_DIR))) {
        return dir;
      }
    }
    throw new IllegalStateException(
        "Could not locate " + INIT_SQL + " and " + FLAGS_DIR + " from " + candidates);
  }
}
