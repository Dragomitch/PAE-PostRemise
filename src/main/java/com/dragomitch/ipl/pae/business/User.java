package com.dragomitch.ipl.pae.business;

import com.dragomitch.ipl.pae.business.dto.UserDto;

public interface User extends UserDto {

  void checkDataIntegrity();

}
