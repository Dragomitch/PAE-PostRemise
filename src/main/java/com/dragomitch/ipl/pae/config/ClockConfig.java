package com.dragomitch.ipl.pae.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The clock the application reads the current date and time from. The use cases and the
 * persistence adapters take it as a dependency instead of calling {@code LocalDateTime.now()}, so
 * that a test can replace it with {@link Clock#fixed} and assert on exact dates.
 */
@Configuration
public class ClockConfig {

  /**
   * The system clock in the JVM's default time zone: what {@code LocalDateTime.now()} reads.
   *
   * @return the clock
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
