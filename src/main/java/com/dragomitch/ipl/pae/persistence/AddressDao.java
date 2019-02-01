package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.AddressDto;

public interface AddressDao {

  public static final String TABLE_NAME = "addresses";
  public static final String COLUMN_ID = "address_id";
  public static final String COLUMN_STREET = "street";
  public static final String COLUMN_NUMBER = "number";
  public static final String COLUMN_POSTAL_CODE = "postal_code";
  public static final String COLUMN_CITY = "city";
  public static final String COLUMN_COUNTRY = "country";
  public static final String COLUMN_REGION = "region";

  int MAX_LENGTH_STREET = 60;
  int MAX_LENGTH_NUMBER = 10;
  int MAX_LENGTH_CITY = 60;
  int MAX_LENGTH_POSTAL_CODE = 10;
  int MAX_LENGTH_REGION = 60;

  /**
   * Inserts an address into the database.
   * 
   * @param address the address to be inserted in the database
   * @return the AddressDto inserted into the DB.
   */
  AddressDto create(AddressDto address);

  /**
   * Retrieve an address with the id passed in parameter.
   * 
   * @param id the id of the Address we want to retrieve from the Database.
   * @return the AddressDto corresponding to the address row having the param id as id.
   */
  AddressDto findById(int id);

  /**
   * Update an row in the Database with the data provided by the address Object. The id of the row
   * affected is the one into the address object.
   * 
   * @param address the new data we want to set for the id
   * @return the addressDto containing the updated data
   */
  AddressDto update(AddressDto address);

}
