package com.dragomitch.ipl.pae;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards against diagnostics that bypass the logging configuration: production code logs through
 * SLF4J ({@code LogManager.getLogger}), never to the console.
 */
class SourceHygieneTest {

  private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

  private static final Pattern CONSOLE_OUTPUT =
      Pattern.compile("System\\s*\\.\\s*(out|err)\\b|\\.printStackTrace\\s*\\(");

  @Test
  void productionCodeNeverWritesToTheConsole() throws IOException {
    List<String> offenders;
    try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
      offenders = files.filter(file -> file.toString().endsWith(".java"))
          .flatMap(SourceHygieneTest::consoleOutputLines).toList();
    }

    assertThat(offenders).isEmpty();
  }

  private static Stream<String> consoleOutputLines(Path file) {
    try {
      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
      return java.util.stream.IntStream.range(0, lines.size())
          .filter(i -> CONSOLE_OUTPUT.matcher(lines.get(i)).find())
          .mapToObj(i -> file + ":" + (i + 1) + ": " + lines.get(i).trim());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
