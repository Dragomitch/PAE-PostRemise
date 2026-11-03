package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Programme;
import java.util.List;

public interface ProgrammeRepository {

  String TABLE_NAME = "programmes";
  String COLUMN_ID = "programme_id";
  String COLUMN_NAME = "name";
  String COLUMN_EXTERNAL_SOFTWARE_NAME = "external_software_name";

  /**
   * Queries the database for a programme identified by an id.
   *
   * @param id : the id of the programme we want to find
   * @return the Programme corresponding to the id
   */
  Programme findById(int id);

  /**
   * Queries the database for all of existing programmes.
   *
   * @return the list of all programmes present in the database
   */
  List<Programme> findAll();
}
