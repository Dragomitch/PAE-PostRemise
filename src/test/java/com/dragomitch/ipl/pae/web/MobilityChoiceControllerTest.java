package com.dragomitch.ipl.pae.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MobilityChoiceController.class)
@Import(WebTestConfig.class)
class MobilityChoiceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private MobilityChoiceUcc mobilityChoiceUcc;

  @Test
  void createBindsTheFormValuesSentAsJson() throws Exception {
    MobilityChoiceDto created = (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    created.setId(9);
    when(mobilityChoiceUcc.create(any(), anyInt(), any())).thenReturn(created);

    // the legacy form serializes every value as a string
    mockMvc.perform(post(ApiPaths.BASE + "/mobilityChoice").with(csrf())
            .with(TestUsers.student()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"preferenceOrder\":\"1\",\"mobilityType\":\"SMS\",\"term\":\"2\","
                + "\"academicYear\":\"2025\",\"programme\":{\"id\":\"1\"},"
                + "\"country\":{\"countryCode\":\"FR\"},\"partner\":{\"id\":\"3\"},"
                + "\"user\":{\"id\":2,\"username\":\"stud\",\"role\":\"Student\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9));

    ArgumentCaptor<MobilityChoiceDto> captor = ArgumentCaptor.forClass(MobilityChoiceDto.class);
    verify(mobilityChoiceUcc).create(captor.capture(), eq(TestUsers.STUDENT_ID),
        eq(UserDto.ROLE_STUDENT));
    MobilityChoiceDto choice = captor.getValue();
    assertEquals(1, choice.getPreferenceOrder());
    assertEquals(2, choice.getTerm());
    assertEquals(2025, choice.getAcademicYear());
    assertEquals("FR", choice.getCountry().getCountryCode());
    assertEquals(3, choice.getPartner().getId());
    assertEquals(2, choice.getUser().getId());
  }

  @Test
  void showAllPassesTheFilterAndWrapsTheListInData() throws Exception {
    MobilityChoiceDto choice = (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    choice.setId(4);
    when(mobilityChoiceUcc.showAll(TestUsers.PROFESSOR_ID, UserDto.ROLE_PROFESSOR, "rejected"))
        .thenReturn(List.of(choice));

    mockMvc.perform(get(ApiPaths.BASE + "/mobilityChoices?filter=rejected&_=12345")
            .with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(4));
  }

  @Test
  void countAnswersACount() throws Exception {
    when(mobilityChoiceUcc.countAll(TestUsers.STUDENT_ID, UserDto.ROLE_STUDENT, null))
        .thenReturn(3);

    mockMvc.perform(get(ApiPaths.BASE + "/mobilityChoices/count").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"count\":3}", true));
  }

  @Test
  void cancelTakesThePlainReasonFromTheQuery() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilityChoices/5/cancel")
            .param("reason", "Trop cher, désolé").with(csrf()).with(TestUsers.student()))
        .andExpect(status().isOk());
    verify(mobilityChoiceUcc).cancel(5, TestUsers.STUDENT_ID, "Trop cher, désolé");
  }

  @Test
  void aUseCasePermissionRefusalIs403() throws Exception {
    org.mockito.Mockito.doThrow(new InsufficientPermissionException())
        .when(mobilityChoiceUcc).cancel(5, TestUsers.STUDENT_ID, "r");
    mockMvc.perform(put(ApiPaths.BASE + "/mobilityChoices/5/cancel?reason=r").with(csrf())
            .with(TestUsers.student()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void rejectTakesTheDenialReasonIdFromTheQuery() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilityChoices/5/reject?reason=2").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isOk());
    verify(mobilityChoiceUcc).reject(5, 2);
  }

  @Test
  void aWronglyTypedParameterIs400() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilityChoices/5/reject?reason=abc").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void confirmIsMadeByTheAuthenticatedProfessor() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilityChoices/5/confirm").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isOk());
    verify(mobilityChoiceUcc).confirm(5, TestUsers.PROFESSOR_ID);
  }

  @Test
  void confirmWithNewPartnerReadsThePartnerFromTheBody() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilityChoices/5/confirmWithNewPartner").with(csrf())
            .with(TestUsers.student()).contentType(MediaType.APPLICATION_JSON)
            .content(TestBodies.PARTNER.replace("\"BE\"", "\"FR\"")))
        .andExpect(status().isOk());

    ArgumentCaptor<PartnerDto> captor = ArgumentCaptor.forClass(PartnerDto.class);
    verify(mobilityChoiceUcc).confirmWithNewPartner(eq(5), captor.capture(),
        eq(TestUsers.STUDENT_ID), eq(UserDto.ROLE_STUDENT));
    assertEquals("ACME", captor.getValue().getFullName());
    assertEquals("FR", captor.getValue().getAddress().getCountry().getCountryCode());
    assertEquals("IT", captor.getValue().getOptions().get(0).getDepartement());
  }

  @Test
  void exportIsUtf8Csv() throws Exception {
    when(mobilityChoiceUcc.exportAll(TestUsers.PROFESSOR_ID, UserDto.ROLE_PROFESSOR, null))
        .thenReturn("﻿N° ordre candidature;Nom;\n1;Dupré;\n");

    byte[] body = mockMvc.perform(get(ApiPaths.BASE + "/mobilityChoices/export")
            .with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv;charset=UTF-8"))
        .andReturn().getResponse().getContentAsByteArray();
    String csv = new String(body, StandardCharsets.UTF_8);
    assertTrue(csv.startsWith("﻿N° ordre"), csv);
    assertTrue(csv.contains("Dupré"), csv);
  }
}
