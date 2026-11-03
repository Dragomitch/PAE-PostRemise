package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Programme;
import java.util.List;

public interface ProgrammeService {

  /**
   * Ask the programmeRepository to complete a programmeDto existing in the database.
   *
   * @param id : the id of the programme we want to find
   * @return the progrmmeDto corresponding to the id
   * @throws com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException if no programme has
   *     this id
   */
  Programme showOne(int id);

  /**
   * Return the list of all programmes.
   *
   * @return the list containing every programme
   */
  List<Programme> showAll();
}
