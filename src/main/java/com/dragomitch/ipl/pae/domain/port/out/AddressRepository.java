package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Address;
import java.util.Optional;

public interface AddressRepository {

  /**
   * Inserts an address into the database.
   *
   * @param address the address to be inserted in the database
   * @return the Address inserted into the DB.
   */
  Address create(Address address);

  /**
   * Retrieve an address with the id passed in parameter.
   *
   * @param id the id of the Address we want to retrieve from the Database.
   * @return the Address corresponding to the address row having the param id as id, or empty if
   *     there is none
   */
  Optional<Address> findById(int id);

  /**
   * Update an row in the Database with the data provided by the address Object. The id of the row
   * affected is the one into the address object.
   *
   * @param address the new data we want to set for the id
   * @return the address containing the updated data
   */
  Address update(Address address);
}
