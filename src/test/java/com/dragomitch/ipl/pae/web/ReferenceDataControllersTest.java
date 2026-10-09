package com.dragomitch.ipl.pae.web;

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

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.uccontrollers.CountryUcc;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;
import com.dragomitch.ipl.pae.uccontrollers.OptionUcc;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The small controllers: countries, programmes, options, denial reasons and payments.
 */
@WebMvcTest({CountryController.class, ProgrammeController.class, OptionController.class,
    DenialReasonController.class, PaymentController.class})
@Import(WebTestConfig.class)
class ReferenceDataControllersTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private CountryUcc countryUcc;
  @MockBean
  private ProgrammeUcc programmeUcc;
  @MockBean
  private OptionUcc optionUcc;
  @MockBean
  private DenialReasonUcc denialReasonUcc;
  @MockBean
  private PaymentUcc paymentUcc;

  @Test
  void countries() throws Exception {
    CountryDto belgium = (CountryDto) entityFactory.build(CountryDto.class);
    belgium.setCountryCode("BE");
    belgium.setName("Belgique");
    when(countryUcc.showAll()).thenReturn(List.of(belgium));
    when(countryUcc.showOne("BE")).thenReturn(belgium);

    mockMvc.perform(get(ApiPaths.BASE + "/countries").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].countryCode").value("BE"));
    mockMvc.perform(get(ApiPaths.BASE + "/countries/BE").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Belgique"));
  }

  @Test
  void programmes() throws Exception {
    ProgrammeDto erasmus = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    erasmus.setId(1);
    erasmus.setProgrammeName("Erasmus+");
    when(programmeUcc.showAll()).thenReturn(List.of(erasmus));
    when(programmeUcc.showOne(1)).thenReturn(erasmus);

    mockMvc.perform(get(ApiPaths.BASE + "/programmes").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].programmeName").value("Erasmus+"));
    mockMvc.perform(get(ApiPaths.BASE + "/programmes/1").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void optionsArePublicAndTheirPartnersAreNot() throws Exception {
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode("BIN");
    option.setName("Informatique");
    when(optionUcc.showAll()).thenReturn(List.of(option));
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(3);
    when(optionUcc.findAllPartnersByOption("BIN")).thenReturn(List.of(partner));

    mockMvc.perform(get(ApiPaths.BASE + "/options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("BIN"))
        .andExpect(jsonPath("$[0].name").value("Informatique"));
    mockMvc.perform(get(ApiPaths.BASE + "/options/BIN").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(3));
  }

  @Test
  void denialReasons() throws Exception {
    DenialReasonDto reason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    reason.setId(4);
    reason.setReason("Dossier incomplet");
    when(denialReasonUcc.create(any())).thenReturn(reason);
    when(denialReasonUcc.showAll()).thenReturn(List.of(reason));

    // the legacy UI posts {reason: "..."} to /denialReasons
    mockMvc.perform(post(ApiPaths.BASE + "/denialReasons").with(csrf())
            .with(TestUsers.professor()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Dossier incomplet\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
    ArgumentCaptor<DenialReasonDto> captor = ArgumentCaptor.forClass(DenialReasonDto.class);
    verify(denialReasonUcc).create(captor.capture());
    assertEquals("Dossier incomplet", captor.getValue().getReason());

    mockMvc.perform(get(ApiPaths.BASE + "/denialReasons").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reason").value("Dossier incomplet"));

    mockMvc.perform(put(ApiPaths.BASE + "/denialReasons/4").with(csrf())
            .with(TestUsers.professor()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"Autre\"}"))
        .andExpect(status().isOk());
    verify(denialReasonUcc).edit(eq(4), any());
  }

  @Test
  void paymentsAreWrappedInData() throws Exception {
    PaymentDto payment = (PaymentDto) entityFactory.build(PaymentDto.class);
    payment.setPaymentDate(LocalDateTime.of(2024, 5, 6, 7, 8, 9));
    when(paymentUcc.showAll(UserDto.ROLE_PROFESSOR)).thenReturn(List.of(payment));

    mockMvc.perform(get(ApiPaths.BASE + "/payments").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].paymentDate").value("2024-05-06T07:08:09"));
  }
}
