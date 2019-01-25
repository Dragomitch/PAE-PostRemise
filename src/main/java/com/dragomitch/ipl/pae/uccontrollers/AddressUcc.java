package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.AddressDto;

public interface AddressUcc {

  /**
   * Create an Address Object in DataBase with the address data in parameter.
   * 
   * @param address an AddressDto containing the data for the address to be created.
   * @return the addressDto object fully constructed from DataBase.
   */
  AddressDto create(AddressDto address);

  /**
   * Modify the address in DataBase with the id present in the address.
   * 
   * @param address an AddressDto containing the modified data for the address.
   * @return the AddressDto of the data present after the query in DataBase.
   */
  AddressDto edit(AddressDto address);
}
