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
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PartnerController.class)
@Import(WebTestConfig.class)
class PartnerControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private PartnerUcc partnerUcc;

  private PartnerDto partner(int id) {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(id);
    partner.setFullName("ACME");
    return partner;
  }

  @Test
  void createReadsThePartnerFromTheBodyWithTheRequesterRole() throws Exception {
    when(partnerUcc.create(any(), eq(UserDto.ROLE_STUDENT))).thenReturn(partner(8));

    mockMvc.perform(post(ApiPaths.BASE + "/partners").with(csrf()).with(TestUsers.student())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestBodies.PARTNER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(8));

    ArgumentCaptor<PartnerDto> captor = ArgumentCaptor.forClass(PartnerDto.class);
    verify(partnerUcc).create(captor.capture(), eq(UserDto.ROLE_STUDENT));
    assertEquals(12, captor.getValue().getEmployeeCount());
    assertEquals("BIN", captor.getValue().getOptions().get(0).getCode());
  }

  @Test
  void showAllPassesTheFilterAndIsWrappedInData() throws Exception {
    when(partnerUcc.showAll(new PartnerSearch("country", "FR"), UserDto.ROLE_STUDENT,
        TestUsers.STUDENT_ID))
        .thenReturn(List.of(partner(1), partner(2)));

    mockMvc.perform(get(ApiPaths.BASE + "/partners?filter=country&value=FR")
            .with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));
  }

  @Test
  void anUnknownPartnerIs404() throws Exception {
    when(partnerUcc.showOne(99)).thenThrow(new ResourceNotFoundException());

    mockMvc.perform(get(ApiPaths.BASE + "/partners/99").with(TestUsers.student()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void editTakesTheIdFromThePath() throws Exception {
    when(partnerUcc.edit(eq(3), any(), eq(UserDto.ROLE_PROFESSOR))).thenReturn(partner(3));

    mockMvc.perform(put(ApiPaths.BASE + "/partners/3").with(csrf()).with(TestUsers.professor())
            .contentType(MediaType.APPLICATION_JSON).content(TestBodies.PARTNER.replace("\"official\":false",
                "\"official\":false,\"archived\":true,\"version\":2")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("ACME"));
  }

  @Test
  void addOptionAndListTheOptions() throws Exception {
    mockMvc.perform(post(ApiPaths.BASE + "/partners/3").with(csrf()).with(TestUsers.student())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"BIN\",\"departement\":\"IT\"}"))
        .andExpect(status().isOk());
    ArgumentCaptor<PartnerOptionDto> captor = ArgumentCaptor.forClass(PartnerOptionDto.class);
    verify(partnerUcc).addOption(eq(3), captor.capture());
    assertEquals("IT", captor.getValue().getDepartement());

    when(partnerUcc.findAllPartnerOption(3)).thenReturn(List.of(captor.getValue()));
    mockMvc.perform(get(ApiPaths.BASE + "/partners/partnersOptions/3")
            .with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("BIN"));
  }

  @Test
  void restoreUsesTheRoleOfTheRequester() throws Exception {
    when(partnerUcc.restore(3, UserDto.ROLE_STUDENT)).thenReturn(partner(3));

    mockMvc.perform(put(ApiPaths.BASE + "/partners/3/restore").with(csrf())
            .with(TestUsers.student()).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3));
  }
}
