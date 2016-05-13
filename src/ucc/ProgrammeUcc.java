package ucc;

import business.dto.ProgrammeDto;

import java.util.List;

public interface ProgrammeUcc {

  /**
   * Ask the programmeDao to complete a programmeDto existing in the database.
   * 
   * @param id : the id of the programme we want to find
   * @return the progrmmeDto corresponding to the id
   */
  ProgrammeDto showOne(int id);

  /**
   * Return the list of all programmes.
   * 
   * @return the list containing every programme
   */
  List<ProgrammeDto> showAll();

}
