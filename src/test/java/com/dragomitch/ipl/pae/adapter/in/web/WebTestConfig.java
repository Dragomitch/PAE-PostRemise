package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.security.SessionCookieService;
import com.dragomitch.ipl.pae.config.JacksonConfig;
import com.dragomitch.ipl.pae.config.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * What a {@code @WebMvcTest} slice needs on top of the controllers, the
 * {@code @RestControllerAdvice} and the {@code WebMvcConfigurer}s it picks up by itself: the real
 * security chain, the JSON mapping of the DTO interfaces (built from the real {@code
 * EntityFactory}) and the session cookie issuer. The use cases are mocked by each test.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({SecurityConfig.class, JacksonConfig.class, SessionCookieService.class})
@ComponentScan("com.dragomitch.ipl.pae.domain.model")
public class WebTestConfig {}
