package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

  @Bean
  public Module entityFactoryTypesModule(EntityFactory entityFactory) {
    return abstractTypesModule(entityFactory);
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
