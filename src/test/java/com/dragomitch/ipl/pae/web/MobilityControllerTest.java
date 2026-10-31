package com.dragomitch.ipl.pae.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.uccontrollers.MobilityUcc;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MobilityController.class)
@Import(WebTestConfig.class)
class MobilityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private MobilityUcc mobilityUcc;

  private MobilityDto mobility() {
    MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
    mobility.setId(4);
    mobility.setVersion(3);
    mobility.setState("En cours");
    mobility.setSubmissionDate(LocalDateTime.of(2024, 2, 1, 10, 15, 30));
    mobility.setFirstPaymentRequestDate(LocalDateTime.of(2024, 3, 2, 8, 0, 0));
    return mobility;
  }

  @Test
  void showAllIsWrappedInDataWithTheDateFormatsTheUiParses() throws Exception {
    when(mobilityUcc.showAll(TestUsers.STUDENT_ID, UserDto.ROLE_STUDENT))
        .thenReturn(List.of(mobility()));

    mockMvc.perform(get(ApiPaths.BASE + "/mobilities").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(4))
        .andExpect(jsonPath("$.data[0].submissionDate").value("2024-02-01 10:15:30"))
        .andExpect(jsonPath("$.data[0].firstPaymentRequestDate").value("2024-03-02T08:00:00"));
  }

  @Test
  void showOnePassesTheRequesterRoleAndId() throws Exception {
    when(mobilityUcc.showOne(4, UserDto.ROLE_STUDENT, TestUsers.STUDENT_ID))
        .thenReturn(mobility());

    mockMvc.perform(get(ApiPaths.BASE + "/mobilities/4").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("En cours"))
        .andExpect(jsonPath("$.version").value(3));
  }

  @Test
  void confirmDocumentTakesTheDocumentAndTheVersionFromTheQuery() throws Exception {
    when(mobilityUcc.confirmDocument(4, 7, 3)).thenReturn(mobility());

    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/confirmDocument?document=7&version=3")
            .with(csrf()).with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
  }

  @Test
  void encodingConfirmationsAndPaymentTakeTheVersion() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/confirmProEcoEncoding?version=3")
        .with(csrf()).with(TestUsers.professor())).andExpect(status().isOk());
    verify(mobilityUcc).confirmProEcoEncoding(4, 3);
    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/confirmSecondSoftwareEncoding?version=3")
        .with(csrf()).with(TestUsers.professor())).andExpect(status().isOk());
    verify(mobilityUcc).confirmSecondSoftwareEncoding(4, 3);
    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/confirmPayment?version=3")
        .with(csrf()).with(TestUsers.professor())).andExpect(status().isOk());
    verify(mobilityUcc).confirmPayment(4, 3);
  }

  @Test
  void aMissingNumberKeepsTheLegacyMinusOne() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/confirmPayment").with(csrf())
        .with(TestUsers.professor())).andExpect(status().isOk());
    verify(mobilityUcc).confirmPayment(4, -1);
  }

  @Test
  void aStaleVersionIsTheConcurrentModificationError() throws Exception {
    when(mobilityUcc.confirmPayment(4, 2)).thenThrow(new ConcurrentModificationException());

    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/confirmPayment?version=2").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
  }

  @Test
  void cancelPassesEveryParameterAndTheRequester() throws Exception {
    when(mobilityUcc.cancel(4, 3, "Malade", -1, TestUsers.STUDENT_ID, UserDto.ROLE_STUDENT))
        .thenReturn(mobility());

    mockMvc.perform(put(ApiPaths.BASE + "/mobilities/4/cancel")
            .param("version", "3").param("cancellationReason", "Malade")
            .with(csrf()).with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
  }

  @Test
  void exportIsCsv() throws Exception {
    when(mobilityUcc.exportDocuments(4, "D")).thenReturn("﻿Nom;Prénom;\n");

    mockMvc.perform(get(ApiPaths.BASE + "/mobilities/4/export?filter=D")
            .with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv;charset=UTF-8"))
        .andExpect(content().string("﻿Nom;Prénom;\n"));
  }
}
