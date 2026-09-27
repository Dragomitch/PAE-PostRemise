package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface NominatedStudentUcc {

  /**
   * Creates a new nominated student.
   * 
   * @param nominatedStudent the student to be created
   * @param userId the id of the currently signed in user
   * @param userRole the role of the currently signed in user
   * @return the newly created nominated user
   */
  NominatedStudentDto create(@NotNull @Valid NominatedStudentDto nominatedStudent,
      @Positive int userId, @NotBlank String userRole);

  /**
   * Returns the nominated student bearing the given id.
   * 
   * @param id the id used to identify the requested student
   * @param userId the id of the currently signed in user
   * @param role the role of the currently signed in user
   * @return the student if one was found, null otherwise
   */
  NominatedStudentDto showOne(@Positive int id, @Positive int userId, @NotBlank String role);

  /**
   * Returns a list containing all of the nominated students in the database.
   * 
   * @return the list
   */
  List<NominatedStudentDto> showAll();

  /**
   * Updates the nominated student personal information.
   * 
   * @param nominatedStudent the student to be created
   * @param userId the id of the currently signed in user
   * @param userRole the role of the currently signed in user
   * @return the nominated student with updated information
   */
  NominatedStudentDto edit(@NotNull @Valid NominatedStudentDto nominatedStudent,
      @Positive int userId, @NotBlank String userRole);

}
