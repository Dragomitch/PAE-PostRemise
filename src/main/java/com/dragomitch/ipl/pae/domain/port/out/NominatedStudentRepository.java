package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import java.util.List;
import java.util.Optional;

public interface NominatedStudentRepository {

  /**
   * Inserts a nominatedStudent into the database.
   *
   * <p>A nominated student is the student part of a user (same id) and shares the version of that
   * user: {@code NominatedStudentService.edit} updates both rows with the same expected version.
   * The row is therefore stored with the version carried by the DTO (the user's), which is also the
   * version of the returned DTO.
   *
   * @param nominatedStudent , the nominatedStudent to be inserted in the database, carrying the id
   *     and the version of its user.
   * @return the nominatedStudent inserted in the database, with the stored version
   */
  public NominatedStudent create(NominatedStudent nominatedStudent);

  /**
   * Queries the database for a nominatedStudent based on it's id.
   *
   * @param id : the id of the nominatedStudent we want to find.
   * @return a nominatedStudent containing the data we were looking for, or empty if there is none
   */
  Optional<NominatedStudent> findById(int id);

  /**
   * Queries the database for all nominatedStudents.
   *
   * @return a list of all nominatedStudent.
   */
  public List<NominatedStudent> findAll();

  /**
   * Update a nominatedStudent in the database.
   *
   * @param nominatedStudent : the nominatedStudent to be updated in the database.
   * @return the nominatedStudent that has just been updated.
   */
  public NominatedStudent update(NominatedStudent nominatedStudent);
}
