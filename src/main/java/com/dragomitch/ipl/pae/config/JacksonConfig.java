package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON mapping of the business objects by Spring MVC's {@code ObjectMapper}.
 *
 * <p>The DTOs are interfaces: this module tells Jackson which implementation to instantiate for a
 * request body or an interface-typed property (e.g. {@code UserDto.option}), using the bindings
 * of the {@link EntityFactory} as the single source of truth. Spring Boot registers every
 * {@link Module} bean in its {@code ObjectMapper}, which also has the Java time module and writes
 * dates as ISO-8601 strings ({@code 2024-02-01T10:15:30}, {@code 2000-12-31} for a
 * {@code LocalDate}), as the web UIs expect.
 *
 * <p>Absent values are left out of the JSON ({@code NON_NULL}), as the original serializer of the
 * API did: the legacy web UI was written for it and tests some optional properties (partner,
 * country, denial reason...) with {@code === undefined}. Its newer checks use {@code == null},
 * which works either way.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

  @Bean
  public Module entityFactoryTypesModule(EntityFactory entityFactory) {
    return abstractTypesModule(entityFactory);
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer leaveNullPropertiesOut() {
    return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static SimpleModule abstractTypesModule(EntityFactory entityFactory) {
    SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
    entityFactory.getImplementations()
        .forEach((iface, impl) -> resolver.addMapping((Class) iface, (Class) impl));
    SimpleModule module = new SimpleModule("EntityFactoryTypes");
    module.setAbstractTypes(resolver);
    return module;
  }
}
