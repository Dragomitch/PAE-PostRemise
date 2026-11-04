package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Address;

public interface AddressService {

  /**
   * Create an Address Object in DataBase with the address data in parameter.
   *
   * @param address an Address containing the data for the address to be created.
   * @return the address object fully constructed from DataBase.
   */
  Address create(Address address);

  /**
   * Modify the address in DataBase with the id present in the address.
   *
   * @param address an Address containing the modified data for the address.
   * @return the Address of the data present after the query in DataBase.
   */
  Address edit(Address address);
}
