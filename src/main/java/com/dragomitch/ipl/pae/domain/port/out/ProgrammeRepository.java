package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Programme;
import java.util.List;
import java.util.Optional;

public interface ProgrammeRepository {

  /**
   * Queries the database for a programme identified by an id.
   *
   * @param id : the id of the programme we want to find
   * @return the Programme corresponding to the id, or empty if there is none
   */
  Optional<Programme> findById(int id);

  /**
   * Queries the database for all of existing programmes.
   *
   * @return the list of all programmes present in the database
   */
  List<Programme> findAll();
}
