package com.dragomitch.ipl.pae;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.mockito.MockedStatic;

/**
 * Simple test ensuring the application main entry point runs without throwing
 * exceptions. Using a plain JUnit test avoids loading the entire Spring
 * application context unnecessarily.
 */
public class ApplicationTests {

  @Test
  public void applicationStarts() {
    try (MockedStatic<SpringApplication> app = Mockito.mockStatic(SpringApplication.class)) {
      Application.main(new String[] {});
      app.verify(() -> SpringApplication.run(Application.class, new String[] {}));
    }
  }
}
