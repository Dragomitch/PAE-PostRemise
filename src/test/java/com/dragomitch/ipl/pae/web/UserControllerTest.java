package com.dragomitch.ipl.pae.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(WebTestConfig.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private UserUcc userUcc;

  private static final String VALID_SIGNUP = "{\"username\":\"jdoe\",\"password\":\"secret123\","
      + "\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"jdoe@example.test\","
      + "\"option\":{\"code\":\"BIN\"}}";

  private UserDto user(int id, String username) {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(id);
    user.setUsername(username);
    user.setPassword("$2a$10$hash");
    user.setRole(UserDto.ROLE_STUDENT);
    user.setRegistrationDate(LocalDateTime.of(2024, 2, 1, 10, 15, 30));
    return user;
  }

  @Test
  void signupReadsTheUserFromTheJsonBodyAndHidesThePassword() throws Exception {
    when(userUcc.signup(any())).thenReturn(user(5, "jdoe"));

    mockMvc.perform(post(ApiPaths.BASE + "/users").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"jdoe\",\"password\":\"secret123\",\"firstName\":\"John\","
                + "\"lastName\":\"Doe\",\"email\":\"jdoe@example.test\","
                + "\"option\":{\"code\":\"BIN\"},\"unknownField\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.username").value("jdoe"))
        .andExpect(jsonPath("$.registrationDate").value("2024-02-01T10:15:30"))
        .andExpect(jsonPath("$.password").doesNotExist());

    ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
    verify(userUcc).signup(captor.capture());
    assertEquals("jdoe", captor.getValue().getUsername());
    assertEquals("secret123", captor.getValue().getPassword());
    assertEquals("John", captor.getValue().getFirstName());
    // interface-typed property instantiated through the EntityFactory bindings
    assertEquals("BIN", captor.getValue().getOption().getCode());
  }

  @Test
  void signupBusinessErrorsAreProblems() throws Exception {
    when(userUcc.signup(any())).thenThrow(new BusinessException(ErrorCode.USERNAME_TAKEN, "jdoe"));

    mockMvc.perform(post(ApiPaths.BASE + "/users").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(VALID_SIGNUP))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"))
        .andExpect(jsonPath("$.detail").value("Le nom d’utilisateur « jdoe » est déjà utilisé."));
  }

  @Test
  void signupChecksThePasswordAndTheOtherConstraintsTogether() throws Exception {
    mockMvc.perform(post(ApiPaths.BASE + "/users").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"jdoe\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
            "password", "firstName", "lastName", "email", "option")));
    verifyNoInteractions(userUcc);
  }

  @Test
  void aMalformedBodyIs400() throws Exception {
    mockMvc.perform(post(ApiPaths.BASE + "/users").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void showAllIsWrappedInData() throws Exception {
    when(userUcc.showAll()).thenReturn(List.of(user(1, "prof"), user(2, "stud")));

    mockMvc.perform(get(ApiPaths.BASE + "/users").with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[1].username").value("stud"))
        .andExpect(jsonPath("$.data[1].password").doesNotExist());
  }

  @Test
  void promoteTakesTheIdFromThePath() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/users/3/promote").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isOk());
    verify(userUcc).promoteToProfessor(3);
  }

  @Test
  void aConcurrentPromotionIsAConflictProblem() throws Exception {
    doThrow(new ConcurrentModificationException()).when(userUcc).promoteToProfessor(3);

    mockMvc.perform(put(ApiPaths.BASE + "/users/3/promote").with(csrf())
            .with(TestUsers.professor()).header("Accept-Language", "en"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.CONCURRENT_MODIFICATION.name()))
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  void aConcurrentPromotionByUsernameIsAConflictProblem() throws Exception {
    when(userUcc.promoteToProfessorByUsername("bob"))
        .thenThrow(new ConcurrentModificationException());

    mockMvc.perform(put(ApiPaths.BASE + "/users/by-username/bob/promote").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.CONCURRENT_MODIFICATION.name()));
  }

  @Test
  void promotingAnUnknownUsernameIsANotFoundProblem() throws Exception {
    when(userUcc.promoteToProfessorByUsername("nobody"))
        .thenThrow(new ResourceNotFoundException());

    mockMvc.perform(put(ApiPaths.BASE + "/users/by-username/nobody/promote").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.name()));
  }

  @Test
  void promotingATooLongUsernameIsAValidationProblem() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/users/by-username/" + "x".repeat(21) + "/promote")
            .with(csrf()).with(TestUsers.professor()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()))
        .andExpect(jsonPath("$.errors[0].field").value("username"))
        .andExpect(jsonPath("$.errors[0].code").value("Size"));
    verifyNoInteractions(userUcc);
  }

  @Test
  void promoteByUsernameTakesTheUsernameFromThePathAndReturnsTheUser() throws Exception {
    UserDto promoted = user(4, "Bob.Dupont");
    promoted.setRole(UserDto.ROLE_PROFESSOR);
    promoted.setVersion(2);
    when(userUcc.promoteToProfessorByUsername("Bob.Dupont")).thenReturn(promoted);

    mockMvc.perform(put(ApiPaths.BASE + "/users/by-username/Bob.Dupont/promote").with(csrf())
            .with(TestUsers.professor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4))
        .andExpect(jsonPath("$.role").value(UserDto.ROLE_PROFESSOR))
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.password").doesNotExist());
    verify(userUcc).promoteToProfessorByUsername("Bob.Dupont");
  }

  @Test
  void promoteByUsernameNeedsTheCsrfToken() throws Exception {
    mockMvc.perform(put(ApiPaths.BASE + "/users/by-username/bob/promote")
            .with(TestUsers.professor()))
        .andExpect(status().isForbidden());
  }

  @Test
  void editPassesTheBodyAndTheAuthenticatedUser() throws Exception {
    when(userUcc.edit(any(), eq(TestUsers.STUDENT_ID), eq(UserDto.ROLE_STUDENT)))
        .thenReturn(user(2, "stud"));

    mockMvc.perform(put(ApiPaths.BASE + "/users/edit").with(csrf()).with(TestUsers.student())
            .contentType(MediaType.APPLICATION_JSON).content("{\"id\":2,\"username\":\"stud\","
                + "\"firstName\":\"S\",\"lastName\":\"T\",\"email\":\"s@t.be\","
                + "\"option\":{\"code\":\"BIN\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("stud"));

    ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
    verify(userUcc).edit(captor.capture(), eq(TestUsers.STUDENT_ID), eq(UserDto.ROLE_STUDENT));
    assertEquals(2, captor.getValue().getId());
  }
}
