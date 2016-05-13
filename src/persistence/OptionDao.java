package persistence;


import business.dto.OptionDto;

import java.util.List;

public interface OptionDao {

  public static final String TABLE_NAME = "options";
  public static final String COLUMN_CODE = "option_code";
  public static final String COLUMN_NAME = "name";

  int OPTION_CODE_LENGTH = 3;

  /**
   * Retrieve an option with the code passed in parameter.
   * 
   * @param code the code of the Option we want to retrieve from the Database.
   * @return the optionDto corresponding to the option row having the param option_code as code.
   */
  OptionDto findByCode(String code);

  /**
   * Queries the database for all options.
   * 
   * @return all denialReasons
   */
  List<OptionDto> findAll();

}
