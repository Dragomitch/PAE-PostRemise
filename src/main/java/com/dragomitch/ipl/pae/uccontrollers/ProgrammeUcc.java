package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;

import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProgrammeUcc {

  /**
   * Ask the programmeDao to complete a programmeDto existing in the database.
   * 
   * @param id : the id of the programme we want to find
   * @return the progrmmeDto corresponding to the id
   */
  ProgrammeDto showOne(@Positive int id);

  /**
   * Return the list of all programmes.
   * 
   * @return the list containing every programme
   */
  List<ProgrammeDto> showAll();

}
