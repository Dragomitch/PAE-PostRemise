package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Option;
import java.util.List;
import java.util.Optional;

public interface OptionRepository {

  /**
   * Retrieve an option with the code passed in parameter.
   *
   * @param code the code of the Option we want to retrieve from the Database.
   * @return the option corresponding to the option row having the param option_code as code, or
   *     empty if there is none
   */
  Optional<Option> findByCode(String code);

  /**
   * Queries the database for all options.
   *
   * @return all denialReasons
   */
  List<Option> findAll();
}
