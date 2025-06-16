package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonSerializer {

  private static final String CONTENT_TYPE = "application/json";

  private final EntityFactory entityFactory;
  private final ObjectMapper mapper;

  @Inject
  public JsonSerializer(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

  public Object deserialize(String str, Class<?> toClass) {
    if (str == null) {
      return null;
    }
    try {
      Object entity = entityFactory.build(toClass);
      return mapper.readerForUpdating(entity).readValue(str);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "Impossible to deserialize the following Json String :" + str, ex);
    }
  }
}
