package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.owlike.genson.Context;
import com.owlike.genson.Converter;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.JsonBindingException;
import com.owlike.genson.convert.ChainedFactory;
import com.owlike.genson.stream.JsonStreamException;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.context.TypeResolver;

public class JsonSerializer {

  private static final String CONTENT_TYPE = "application/json";

  private EntityFactory entityFactory;
  private Genson genson;

  @Inject
  public JsonSerializer(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
    initializeGensonBuilder();
  }

  /**
   * Returns serialization MIME Type.
   *
   * @return MIME Type
   */
  public String getContentType() {
    return CONTENT_TYPE;
  }

  /**
   * Creates a GensonBuilder instance that will be in charge of object serialization/deserialization.
   */
  private void initializeGensonBuilder() {
    this.genson = new GensonBuilder().useIndentation(true).useRuntimeType(true).useMetadata(true)
        .useConstructorWithArguments(false).useMethods(true).useFields(false).setSkipNull(true)
        .withConverter(new LocalDateConverter(), LocalDate.class)
        .withConverter(new LocalDateTimeConverter(), LocalDateTime.class)
        .withConverterFactory(new ConverterFactory()).useRuntimeType(false).create();
  }

  /**
   * Serializes a POJO into a JSON string.
   *
   * @param obj the object to serialize
   * @return the serialized object as a JSON string
   */
  public String serialize(Object obj) {
    return genson.serialize(obj);
  }

  /**
   * Deserializes a JSON String into an instance of toClass.
   *
   * @param str the string to deserialize
   * @param toClass type into which to deserialize
   * @return the deserialized object
   * @throws BusinessException when parsing invalid JSON or when deserialization has failed
   */
  public Object deserialize(String str, Class<?> toClass) {
    if (Boolean.class == toClass || boolean.class == toClass) {
      return Boolean.parseBoolean(str);
    }
    if (Byte.class == toClass || byte.class == toClass) {
      return Byte.parseByte(str);
    }
    if (Character.class == toClass || char.class == toClass) {
      return str.charAt(0);
    }
    if (Short.class == toClass || short.class == toClass) {
      if (str == null) {
        return -1;
      }
      return Short.parseShort(str);
    }
    if (Integer.class == toClass || int.class == toClass) {
      if (str == null) {
        return -1;
      }
      return Integer.parseInt(str);
    }
    if (Long.class == toClass || long.class == toClass) {
      if (str == null) {
        return -1;
      }
      return Long.parseLong(str);
    }
    if (Float.class == toClass || float.class == toClass) {
      if (str == null) {
        return -1;
      }
      return Float.parseFloat(str);
    }
    if (Double.class == toClass || double.class == toClass) {
      if (str == null) {
        return -1;
      }
      return Double.parseDouble(str);
    }
    if (String.class == toClass) {
      return str;
    }
    if (str == null) {
      return null;
    }
    try {
      Object entity = entityFactory.build(toClass);
      return genson.deserializeInto(str, entity);
    } catch (JsonStreamException | JsonBindingException ex) {
      throw new IllegalArgumentException("Impossible to deserialize the following Json String :" + str, ex);
    }
  }

  private static class ConverterFactory extends ChainedFactory {

    @Override
    protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter) {
      Class<?> klass = TypeResolver.resolve(type);
      if (klass != null) {
        return next().create(klass, genson);
      }
      return nextConverter;
    }

  }

  private static class LocalDateConverter implements Converter<LocalDate> {

    @Override
    public LocalDate deserialize(ObjectReader reader, Context ctx) throws Exception {
      LocalDate parsedDate = LocalDate.parse(reader.valueAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
      return parsedDate;
    }

    @Override
    public void serialize(LocalDate object, ObjectWriter writer, Context ctx) throws Exception {
      writer.writeValue(object.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

  }

  private static class LocalDateTimeConverter implements Converter<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(ObjectReader reader, Context ctx) throws Exception {
      LocalDateTime parsedDate = LocalDateTime.parse(reader.valueAsString(),
          DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      return parsedDate;
    }

    @Override
    public void serialize(LocalDateTime object, ObjectWriter writer, Context ctx) throws Exception {
      writer.writeValue(object.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

  }
}
