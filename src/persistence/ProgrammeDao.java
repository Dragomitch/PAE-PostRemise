package persistence;

import business.dto.ProgrammeDto;

import java.util.List;

public interface ProgrammeDao {

  String TABLE_NAME = "programmes";
  String COLUMN_ID = "programme_id";
  String COLUMN_NAME = "name";
  String COLUMN_EXTERNAL_SOFTWARE_NAME = "external_software_name";

  /**
   * Queries the database for a programme identified by an id.
   * 
   * @param id : the id of the programme we want to find
   * @return the ProgrammeDto corresponding to the id
   */
  ProgrammeDto findById(int id);

  /**
   * Queries the database for all of existing programmes.
   * 
   * @return the list of all programmes present in the database
   */
  List<ProgrammeDto> findAll();

}
