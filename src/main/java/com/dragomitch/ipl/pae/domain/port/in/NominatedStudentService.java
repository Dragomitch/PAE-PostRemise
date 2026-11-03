package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;

public interface NominatedStudentService {

  /**
   * Creates a new nominated student.
   *
   * @param nominatedStudent the student to be created
   * @param userId the id of the currently signed in user
   * @param userRole the role of the currently signed in user
   * @return the newly created nominated user
   */
  NominatedStudent create(NominatedStudent nominatedStudent, int userId, Role userRole);

  /**
   * Returns the nominated student bearing the given id.
   *
   * @param id the id used to identify the requested student
   * @param userId the id of the currently signed in user
   * @param role the role of the currently signed in user
   * @return the student if one was found, null otherwise
   */
  NominatedStudent showOne(int id, int userId, Role role);

  /**
   * Returns a list containing all of the nominated students in the database.
   *
   * @return the list
   */
  List<NominatedStudent> showAll();

  /**
   * Updates the nominated student personal information.
   *
   * @param nominatedStudent the student to be created
   * @param userId the id of the currently signed in user
   * @param userRole the role of the currently signed in user
   * @return the nominated student with updated information
   */
  NominatedStudent edit(NominatedStudent nominatedStudent, int userId, Role userRole);
}
