package com.dragomitch.ipl.pae.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.UserDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring MVC's {@code JsonMapper} reads and writes the web DTOs (records) the way the web UIs
 * expect: no {@code null} property, ISO dates, a write-only password, the numbers and the {@code
 * null}s of the legacy forms.
 */
@JsonTest
@Import(JacksonConfig.class)
class JacksonConfigTest {

  @Autowired private JsonMapper mapper;

  @Test
  void everyWebDtoCanBeReadFromAnEmptyObject() throws Exception {
    for (Class<?> type :
        List.of(
            com.dragomitch.ipl.pae.adapter.in.web.dto.AddressDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.CountryDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.DenialReasonDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.DocumentDto.class,
            MobilityChoiceDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityDto.class,
            NominatedStudentDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.OptionDto.class,
            PartnerDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerOptionDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.PaymentDto.class,
            com.dragomitch.ipl.pae.adapter.in.web.dto.ProgrammeDto.class,
            UserDto.class)) {
      assertTrue(type.isInstance(mapper.readValue("{}", type)), type.getName());
    }
  }

  @Test
  void nestedRecordsAndThePasswordAreRead() throws Exception {
    UserDto user =
        mapper.readValue(
            "{\"username\":\"jdoe\",\"password\":\"secret123\",\"option\":{\"code\":\"BIN\"}}",
            UserDto.class);
    assertEquals("jdoe", user.username());
    assertEquals("secret123", user.password());
    assertNotNull(user.option());
    assertEquals("BIN", user.option().code());
  }

  @Test
  void thePasswordIsNeverWritten() throws Exception {
    UserDto user = new UserDto(0, null, null, "jdoe", "secret123", null, null, null, null, 0);
    String json = mapper.writeValueAsString(user);
    assertFalse(json.contains("secret123"), json);
    assertFalse(json.contains("\"password\""), json);
  }

  @Test
  void absentPropertiesAreLeftOut() throws Exception {
    MobilityChoiceDto choice =
        mapper.readValue("{\"mobilityType\":\"SMS\"}", MobilityChoiceDto.class);

    String json = mapper.writeValueAsString(choice);

    // the legacy web UI tests some optional properties with "=== undefined"
    assertTrue(json.contains("\"mobilityType\":\"SMS\""), json);
    assertFalse(json.contains("partner"), json);
    assertFalse(json.contains("null"), json);
  }

  @Test
  void datesAreIsoStrings() throws Exception {
    UserDto user =
        new UserDto(
            0,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.of(2024, 2, 1, 10, 15, 30),
            null,
            0);
    assertTrue(
        mapper.writeValueAsString(user).contains("\"registrationDate\":\"2024-02-01T10:15:30\""));

    NominatedStudentDto student =
        mapper.readValue("{\"birthdate\":\"2000-12-31\"}", NominatedStudentDto.class);
    assertEquals(LocalDate.of(2000, 12, 31), student.birthdate());
    assertTrue(mapper.writeValueAsString(student).contains("\"birthdate\":\"2000-12-31\""));
  }

  @Test
  void numbersSentAsStringsByTheLegacyFormsAreAccepted() throws Exception {
    MobilityChoiceDto choice =
        mapper.readValue(
            "{\"term\":\"1\",\"academicYear\":\"2025\",\"partner\":{\"id\":\"-1\"}}",
            MobilityChoiceDto.class);
    assertEquals(1, choice.term());
    assertEquals(2025, choice.academicYear());
    assertEquals(-1, choice.partner().id());
  }

  @Test
  void nullSentForAPrimitivePropertyIsReadAsZero() throws Exception {
    // Jackson 3 refuses it by default (FAIL_ON_NULL_FOR_PRIMITIVES); Jackson 2 read it as 0, and
    // the legacy forms send it (spring.jackson.deserialization.fail-on-null-for-primitives=false)
    MobilityChoiceDto choice =
        mapper.readValue(
            "{\"id\":null,\"term\":null,\"academicYear\":2025,\"version\":null}",
            MobilityChoiceDto.class);
    assertEquals(0, choice.id());
    assertEquals(0, choice.term());
    assertEquals(2025, choice.academicYear());
    assertEquals(0, choice.version());
    PartnerDto partner =
        mapper.readValue("{\"official\":null,\"employeeCount\":null}", PartnerDto.class);
    assertFalse(partner.official());
    assertEquals(0, partner.employeeCount());
  }
}
