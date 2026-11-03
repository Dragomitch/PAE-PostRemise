package com.dragomitch.ipl.pae.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON mapping of the web DTOs (the records of {@code adapter/in/web/dto}) by Spring MVC's {@code
 * JsonMapper} (Jackson 3), which reads and writes the {@code java.time} types natively and writes
 * dates as ISO-8601 strings ({@code 2024-02-01T10:15:30}, {@code 2000-12-31} for a {@code
 * LocalDate}), as the web UIs expect.
 *
 * <p>Absent values are left out of the JSON ({@code NON_NULL}), as the original serializer of the
 * API did: the legacy web UI was written for it and tests some optional properties (partner,
 * country, denial reason...) with {@code === undefined}. Its newer checks use {@code == null},
 * which works either way.
 *
 * <p>A {@code null} sent for a primitive property (an {@code int} id, term, version...) is read as
 * {@code 0}, as with Jackson 2, rather than refused: see {@code
 * spring.jackson.deserialization.fail-on-null-for-primitives} in {@code application.properties}.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

  @Bean
  public JsonMapperBuilderCustomizer leaveNullPropertiesOut() {
    return builder ->
        builder.changeDefaultPropertyInclusion(inclusion ->
            inclusion
                .withValueInclusion(JsonInclude.Include.NON_NULL)
                .withContentInclusion(JsonInclude.Include.NON_NULL));
  }
}
