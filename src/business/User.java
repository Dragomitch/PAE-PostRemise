package business;

import business.dto.UserDto;

public interface User extends UserDto {

  void checkDataIntegrity();

}
