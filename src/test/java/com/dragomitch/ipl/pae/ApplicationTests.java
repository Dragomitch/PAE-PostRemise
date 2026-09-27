package com.dragomitch.ipl.pae;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Checks that the Spring Boot entry point delegates to {@link SpringApplication} with the
 * command-line arguments. {@code SpringApplication.run} is mocked so no application context
 * (and therefore no database) is needed.
 */
class ApplicationTests {

  @Test
  void mainDelegatesToSpringApplication() {
    String[] args = {"--spring.profiles.active=test"};
    try (MockedStatic<SpringApplication> app = Mockito.mockStatic(SpringApplication.class)) {
      Application.main(args);
      app.verify(() -> SpringApplication.run(Application.class, args));
    }
  }
}
