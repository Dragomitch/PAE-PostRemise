package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class JsonSerializer {

  private static final String CONTENT_TYPE = "application/json";

  private final EntityFactory entityFactory;
  private final ObjectMapper mapper;

  public JsonSerializer(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.mapper.registerModule(abstractTypesModule(entityFactory));
  }

  /**
   * Lets Jackson instantiate interface-typed properties (e.g. {@code UserDto.option}) with the
   * implementation bound in the {@link EntityFactory}.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static SimpleModule abstractTypesModule(EntityFactory entityFactory) {
    SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
    entityFactory.getImplementations()
        .forEach((iface, impl) -> resolver.addMapping((Class) iface, (Class) impl));
    SimpleModule module = new SimpleModule("EntityFactoryTypes");
    module.setAbstractTypes(resolver);
    return module;
  }

  public String getContentType() {
    return CONTENT_TYPE;
  }

  public String serialize(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Unable to serialize object", ex);
    }
  }

  /**
   * Converts a request value into an argument of type {@code toClass}. Scalars are parsed from
   * their plain string form (a missing number becomes -1, as the legacy routes expect), business
   * objects are built by the {@link EntityFactory} and filled from JSON, and any other type is
   * read as JSON.
   *
   * @param str the raw value, may be null
   * @param toClass the type of the argument
   * @return the converted value
   * @throws NumberFormatException if a numeric value cannot be parsed
   * @throws IllegalArgumentException if the JSON cannot be read
   */
  public Object deserialize(String str, Class<?> toClass) {
    if (String.class == toClass) {
      return str;
    }
    if (Boolean.class == toClass || boolean.class == toClass) {
      return Boolean.parseBoolean(str);
    }
    if (Character.class == toClass || char.class == toClass) {
      return str == null || str.isEmpty() ? null : str.charAt(0);
    }
    if (Byte.class == toClass || byte.class == toClass) {
      return str == null ? (byte) -1 : Byte.parseByte(str);
    }
    if (Short.class == toClass || short.class == toClass) {
      return str == null ? (short) -1 : Short.parseShort(str);
    }
    if (Integer.class == toClass || int.class == toClass) {
      return str == null ? -1 : Integer.parseInt(str);
    }
    if (Long.class == toClass || long.class == toClass) {
      return str == null ? -1L : Long.parseLong(str);
    }
    if (Float.class == toClass || float.class == toClass) {
      return str == null ? -1f : Float.parseFloat(str);
    }
    if (Double.class == toClass || double.class == toClass) {
      return str == null ? -1d : Double.parseDouble(str);
    }
    if (str == null) {
      return null;
    }
    try {
      if (entityFactory.getImplementations().containsKey(toClass)) {
        Object entity = entityFactory.build(toClass);
        return mapper.readerForUpdating(entity).readValue(str);
      }
      return mapper.readValue(str, toClass);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Impossible to deserialize the following Json String :" + str, ex);
    }
  }
}
