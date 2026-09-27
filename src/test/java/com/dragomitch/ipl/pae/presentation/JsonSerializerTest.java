package com.dragomitch.ipl.pae.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
class JsonSerializerTest {

  @Autowired
  private EntityFactory entityFactory;

  private JsonSerializer serializer;

  @BeforeEach
  void setUp() {
    serializer = new JsonSerializer(entityFactory);
  }

  @Test
  void deserializesInterfaceTypedPropertiesAndThePassword() {
    UserDto user = (UserDto) serializer.deserialize(
        "{\"username\":\"jdoe\",\"password\":\"secret123\",\"option\":{\"code\":\"BIN\"}}",
        UserDto.class);

    assertEquals("jdoe", user.getUsername());
    assertEquals("secret123", user.getPassword());
    assertNotNull(user.getOption());
    assertEquals("BIN", user.getOption().getCode());
  }

  @Test
  void neverSerializesThePassword() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setUsername("jdoe");
    user.setPassword("secret123");

    String json = serializer.serialize(user);

    assertFalse(json.contains("secret123"), json);
    assertFalse(json.contains("\"password\""), json);
  }

  @Test
  void convertsPlainRequestValuesToScalars() {
    // Path ids and form fields (e.g. signin's username) are not JSON documents.
    assertEquals("prof1", serializer.deserialize("prof1", String.class));
    assertEquals(42, serializer.deserialize("42", int.class));
    assertEquals(7L, serializer.deserialize("7", Long.class));
    assertEquals(true, serializer.deserialize("true", boolean.class));
    assertEquals(1.5d, serializer.deserialize("1.5", double.class));
  }

  @Test
  void missingNumbersKeepTheLegacyMinusOneDefault() {
    assertEquals(-1, serializer.deserialize(null, int.class));
    assertEquals(null, serializer.deserialize(null, String.class));
    assertEquals(null, serializer.deserialize(null, UserDto.class));
  }

  @Test
  void rejectsMalformedNumbers() {
    assertThrows(NumberFormatException.class, () -> serializer.deserialize("abc", int.class));
  }
}
