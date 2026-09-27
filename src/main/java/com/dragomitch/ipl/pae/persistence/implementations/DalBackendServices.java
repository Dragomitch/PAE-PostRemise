package com.dragomitch.ipl.pae.persistence.implementations;

import java.sql.PreparedStatement;

interface DalBackendServices {

  /**
   * The PostgreSQL schema holding every table of the application (see SQLRessources/init.sql).
   */
  String SCHEMA_NAME = "student_exchange_tools";

  /**
   * Prepares a statement.
   * 
   * @param sql the SQL query
   * @return a PreparedStatement corresponding to the given query
   */
  PreparedStatement prepareStatement(String sql);

}
