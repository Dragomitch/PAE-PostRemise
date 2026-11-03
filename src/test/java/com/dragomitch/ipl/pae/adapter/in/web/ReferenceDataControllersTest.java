package com.dragomitch.ipl.pae.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.in.PaymentService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The small controllers: countries, programmes, options, denial reasons and payments. */
@WebMvcTest({
  CountryController.class,
  ProgrammeController.class,
  OptionController.class,
  DenialReasonController.class,
  PaymentController.class
})
@Import(WebTestConfig.class)
class ReferenceDataControllersTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CountryService countryService;
  @MockitoBean private ProgrammeService programmeService;
  @MockitoBean private OptionService optionService;
  @MockitoBean private DenialReasonService denialReasonService;
  @MockitoBean private PaymentService paymentService;

  @Test
  void countries() throws Exception {
    Country belgium = new Country();
    belgium.setCountryCode("BE");
    belgium.setName("Belgique");
    when(countryService.showAll()).thenReturn(List.of(belgium));
    when(countryService.showOne("BE")).thenReturn(belgium);

    mockMvc
        .perform(get(ApiPaths.BASE + "/countries").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].countryCode").value("BE"));
    mockMvc
        .perform(get(ApiPaths.BASE + "/countries/BE").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Belgique"));
  }

  @Test
  void programmes() throws Exception {
    Programme erasmus = new Programme();
    erasmus.setId(1);
    erasmus.setProgrammeName("Erasmus+");
    when(programmeService.showAll()).thenReturn(List.of(erasmus));
    when(programmeService.showOne(1)).thenReturn(erasmus);

    mockMvc
        .perform(get(ApiPaths.BASE + "/programmes").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].programmeName").value("Erasmus+"));
    mockMvc
        .perform(get(ApiPaths.BASE + "/programmes/1").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void optionsArePublicAndTheirPartnersAreNot() throws Exception {
    Option option = new Option();
    option.setCode("BIN");
    option.setName("Informatique");
    when(optionService.showAll()).thenReturn(List.of(option));
    Partner partner = new Partner();
    partner.setId(3);
    when(optionService.findAllPartnersByOption("BIN")).thenReturn(List.of(partner));

    mockMvc
        .perform(get(ApiPaths.BASE + "/options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("BIN"))
        .andExpect(jsonPath("$[0].name").value("Informatique"));
    mockMvc
        .perform(get(ApiPaths.BASE + "/options/BIN").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(3));
  }

  @Test
  void denialReasons() throws Exception {
    DenialReason reason = new DenialReason();
    reason.setId(4);
    reason.setReason("Dossier incomplet");
    when(denialReasonService.create(any())).thenReturn(reason);
    when(denialReasonService.showAll()).thenReturn(List.of(reason));

    // the legacy UI posts {reason: "..."} to /denialReasons
    mockMvc
        .perform(
            post(ApiPaths.BASE + "/denialReasons")
                .with(csrf())
                .with(TestUsers.professor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Dossier incomplet\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
    ArgumentCaptor<DenialReason> captor = ArgumentCaptor.forClass(DenialReason.class);
    verify(denialReasonService).create(captor.capture());
    assertEquals("Dossier incomplet", captor.getValue().getReason());

    mockMvc
        .perform(get(ApiPaths.BASE + "/denialReasons").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reason").value("Dossier incomplet"));

    mockMvc
        .perform(
            put(ApiPaths.BASE + "/denialReasons/4")
                .with(csrf())
                .with(TestUsers.professor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Autre\"}"))
        .andExpect(status().isOk());
    verify(denialReasonService).edit(eq(4), any());
  }

  @Test
  void paymentsAreWrappedInData() throws Exception {
    Payment payment = new Payment();
    payment.setPaymentDate(LocalDateTime.of(2024, 5, 6, 7, 8, 9));
    when(paymentService.showAll(Role.PROFESSOR)).thenReturn(List.of(payment));

    mockMvc
        .perform(get(ApiPaths.BASE + "/payments").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].paymentDate").value("2024-05-06T07:08:09"));
  }
}
