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
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NominatedStudentController.class)
@Import(WebTestConfig.class)
class NominatedStudentControllerTest {

  private static final String BODY = TestBodies.NOMINATED_STUDENT.replace("\"id\":1", "\"id\":2");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private NominatedStudentUcc nominatedStudentUcc;

  private NominatedStudentDto student() {
    NominatedStudentDto student =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    student.setId(2);
    student.setBirthdate(LocalDate.of(2000, 12, 31));
    return student;
  }

  @Test
  void createReadsTheJsonBody() throws Exception {
    when(nominatedStudentUcc.create(any(), eq(TestUsers.STUDENT_ID), eq(UserDto.ROLE_STUDENT)))
        .thenReturn(student());

    mockMvc.perform(post(ApiPaths.BASE + "/nominatedStudents").with(csrf())
            .with(TestUsers.student()).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.birthdate").value("2000-12-31"));

    ArgumentCaptor<NominatedStudentDto> captor =
        ArgumentCaptor.forClass(NominatedStudentDto.class);
    verify(nominatedStudentUcc).create(captor.capture(), eq(TestUsers.STUDENT_ID),
        eq(UserDto.ROLE_STUDENT));
    assertEquals(LocalDate.of(2000, 12, 31), captor.getValue().getBirthdate());
    assertEquals("BE", captor.getValue().getAddress().getCountry().getCountryCode());
    assertEquals("BE", captor.getValue().getNationality().getCountryCode());
  }

  @Test
  void showOneAndShowAll() throws Exception {
    when(nominatedStudentUcc.showOne(2, TestUsers.STUDENT_ID, UserDto.ROLE_STUDENT))
        .thenReturn(student());
    when(nominatedStudentUcc.showAll()).thenReturn(List.of(student()));

    mockMvc.perform(get(ApiPaths.BASE + "/nominatedStudents/2").with(TestUsers.student()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2));
    // a plain array (no data wrapper), as before
    mockMvc.perform(get(ApiPaths.BASE + "/nominatedStudents").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(2));
  }

  @Test
  void editUpdatesTheStudentOfThePath() throws Exception {
    when(nominatedStudentUcc.edit(any(), eq(TestUsers.PROFESSOR_ID), eq(UserDto.ROLE_PROFESSOR)))
        .thenReturn(student());

    mockMvc.perform(put(ApiPaths.BASE + "/nominatedStudents/5").with(csrf())
            .with(TestUsers.professor()).contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isOk());

    ArgumentCaptor<NominatedStudentDto> captor =
        ArgumentCaptor.forClass(NominatedStudentDto.class);
    verify(nominatedStudentUcc).edit(captor.capture(), eq(TestUsers.PROFESSOR_ID),
        eq(UserDto.ROLE_PROFESSOR));
    assertEquals(5, captor.getValue().getId());
  }
}
