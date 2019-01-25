package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;

import java.util.List;

public interface NominatedStudentUcc {

  /**
   * Creates a new nominated student.
   * 
   * @param nominatedStudent the student to be created
   * @param userId the id of the currently signed in user
   * @param userRole the role of the currently signed in user
   * @return the newly created nominated user
   */
  NominatedStudentDto create(NominatedStudentDto nominatedStudent, int userId, String userRole);

  /**
   * Returns the nominated student bearing the given id.
   * 
   * @param id the id used to identify the requested student
   * @param userId the id of the currently signed in user
   * @param role the role of the currently signed in user
   * @return the student if one was found, null otherwise
   */
  NominatedStudentDto showOne(int id, int userId, String role);

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
  NominatedStudentDto edit(NominatedStudentDto nominatedStudent, int userId, String userRole);

}
