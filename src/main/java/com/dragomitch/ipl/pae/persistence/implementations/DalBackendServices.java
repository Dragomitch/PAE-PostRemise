package com.dragomitch.ipl.pae.persistence.implementations;

import java.sql.PreparedStatement;

interface DalBackendServices {

  /**
   * Prepares a statement.
   * 
   * @param sql the SQL query
   * @return a PreparedStatement corresponding to the given query
   */
  PreparedStatement prepareStatement(String sql);

}
