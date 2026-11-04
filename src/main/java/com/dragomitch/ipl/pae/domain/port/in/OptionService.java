package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import java.util.List;

public interface OptionService {

  /**
   * Return a list of all stored options.
   *
   * @return a list of options
   */
  List<Option> showAll();

  /**
   * Returns a list containing all of the partners in the database having a specific option code.
   *
   * @param optionCode the optionCode of the partners we want to find
   * @return the list
   */
  List<Partner> findAllPartnersByOption(String optionCode);
}
