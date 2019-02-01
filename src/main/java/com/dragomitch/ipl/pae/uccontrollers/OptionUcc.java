package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;

import java.util.List;

public interface OptionUcc {

  /**
   * Return a list of all stored options.
   * 
   * @return a list of options
   */
  List<OptionDto> showAll();

  /**
   * Returns a list containing all of the partners in the database having a specific option code.
   * 
   * @param optionCode the optionCode of the partners we want to find
   * @return the list
   */
  List<PartnerDto> findAllPartnersByOption(String optionCode);
}
