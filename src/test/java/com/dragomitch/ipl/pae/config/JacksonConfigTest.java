package com.dragomitch.ipl.pae.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring MVC's {@code JsonMapper} reads the DTO interfaces through the {@link EntityFactory}
 * bindings and writes the dates the way the web UIs parse them.
 */
@JsonTest
@Import(JacksonConfig.class)
@ComponentScan("com.dragomitch.ipl.pae.domain.model")
class JacksonConfigTest {

  @Autowired
  private JsonMapper mapper;

  @Autowired
  private EntityFactory entityFactory;

  @Test
  void everyDtoInterfaceCanBeRead() throws Exception {
    for (Class<?> type : entityFactory.getImplementations().keySet()) {
      Object read = mapper.readValue("{}", type);
      assertTrue(type.isInstance(read), type.getName());
      assertEquals(entityFactory.getImplementations().get(type), read.getClass());
    }
  }

  @Test
  void interfaceTypedPropertiesAndThePasswordAreRead() throws Exception {
    UserDto user = mapper.readValue(
        "{\"username\":\"jdoe\",\"password\":\"secret123\",\"option\":{\"code\":\"BIN\"}}",
        UserDto.class);
    assertEquals("jdoe", user.getUsername());
    assertEquals("secret123", user.getPassword());
    assertNotNull(user.getOption());
    assertEquals("BIN", user.getOption().getCode());
  }

  @Test
  void thePasswordIsNeverWritten() throws Exception {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setUsername("jdoe");
    user.setPassword("secret123");
    String json = mapper.writeValueAsString(user);
    assertFalse(json.contains("secret123"), json);
    assertFalse(json.contains("\"password\""), json);
  }

  @Test
  void absentPropertiesAreLeftOut() throws Exception {
    MobilityChoiceDto choice = (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    choice.setMobilityType("SMS");

    String json = mapper.writeValueAsString(choice);

    // the legacy web UI tests some optional properties with "=== undefined"
    assertTrue(json.contains("\"mobilityType\":\"SMS\""), json);
    assertFalse(json.contains("partner"), json);
    assertFalse(json.contains("null"), json);
  }

  @Test
  void datesAreIsoStrings() throws Exception {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setRegistrationDate(LocalDateTime.of(2024, 2, 1, 10, 15, 30));
    assertTrue(mapper.writeValueAsString(user)
        .contains("\"registrationDate\":\"2024-02-01T10:15:30\""));

    NominatedStudentDto student = mapper.readValue("{\"birthdate\":\"2000-12-31\"}",
        NominatedStudentDto.class);
    assertEquals(LocalDate.of(2000, 12, 31), student.getBirthdate());
    assertTrue(mapper.writeValueAsString(student).contains("\"birthdate\":\"2000-12-31\""));
  }

  @Test
  void numbersSentAsStringsByTheLegacyFormsAreAccepted() throws Exception {
    MobilityChoiceDto choice = mapper.readValue(
        "{\"term\":\"1\",\"academicYear\":\"2025\",\"partner\":{\"id\":\"-1\"}}",
        MobilityChoiceDto.class);
    assertEquals(1, choice.getTerm());
    assertEquals(2025, choice.getAcademicYear());
    assertEquals(-1, choice.getPartner().getId());
  }

  @Test
  void nullSentForAPrimitivePropertyIsReadAsZero() throws Exception {
    // Jackson 3 refuses it by default (FAIL_ON_NULL_FOR_PRIMITIVES); Jackson 2 read it as 0, and
    // the legacy forms send it (spring.jackson.deserialization.fail-on-null-for-primitives=false)
    MobilityChoiceDto choice = mapper.readValue(
        "{\"id\":null,\"term\":null,\"academicYear\":2025,\"version\":null}",
        MobilityChoiceDto.class);
    assertEquals(0, choice.getId());
    assertEquals(0, choice.getTerm());
    assertEquals(2025, choice.getAcademicYear());
    assertEquals(0, choice.getVersion());
  }
}
