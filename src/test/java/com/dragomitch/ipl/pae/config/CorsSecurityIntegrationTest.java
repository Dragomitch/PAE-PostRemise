package com.dragomitch.ipl.pae.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checks {@link CorsConfig} against the Spring Security filter chain Spring Boot sets up by
 * default (every request authenticated), with the filters registered in their real order. Only
 * the web and security auto-configurations are loaded, so no database is needed.
 */
@SpringBootTest(classes = {CorsConfig.class, CorsSecurityIntegrationTest.PingController.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class, WebMvcAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
    JacksonAutoConfiguration.class})
@AutoConfigureMockMvc
class CorsSecurityIntegrationTest {

  private static final String DEV_ORIGIN = "http://localhost:4200";
  private static final String PATH = "/api/1.0/ping";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void preflightIsNotBlockedBySpringSecurity() throws Exception {
    mockMvc.perform(options(PATH)
            .header(HttpHeaders.ORIGIN, DEV_ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void preflightFromUnknownOriginIsForbidden() throws Exception {
    mockMvc.perform(options(PATH)
            .header(HttpHeaders.ORIGIN, "http://evil.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void securityStillRequiresAuthenticationButTheBrowserCanReadTheAnswer() throws Exception {
    mockMvc.perform(get(PATH).header(HttpHeaders.ORIGIN, DEV_ORIGIN))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN));
  }

  /** Stand-in endpoint under the API prefix. */
  @RestController
  static class PingController {
    @GetMapping(PATH)
    String ping() {
      return "pong";
    }
  }
}
