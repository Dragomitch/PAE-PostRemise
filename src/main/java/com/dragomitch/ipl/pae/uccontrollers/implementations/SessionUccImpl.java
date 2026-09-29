package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import org.slf4j.Logger;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.dragomitch.ipl.pae.persistence.DalServices;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.presentation.annotations.ApiCollection;
import com.dragomitch.ipl.pae.presentation.annotations.HttpParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.annotations.Session;
import com.dragomitch.ipl.pae.presentation.annotations.SessionParameter;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.presentation.exceptions.UnauthenticatedUserException;
import org.springframework.stereotype.Service;

@ApiCollection(name = "Session", endpoint = "/session")
@Service
class SessionUccImpl implements SessionUcc {

  private static Logger logger = LogManager.getLogger(SessionUccImpl.class.getName());

  private UserDao userDao;
  private DalServices dalServices;
  private PasswordEncoder passwordEncoder;

  public SessionUccImpl(UserDao userDao, DalServices dalServices, PasswordEncoder passwordEncoder) {
    this.userDao = userDao;
    this.dalServices = dalServices;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Session(action = Session.SET, attributeNames = {USER_ID, USER_ROLE}, fieldNames = {"id", "role"})
  @Route(method = HttpMethod.POST)
  public UserDto signin(@HttpParameter("username") String username,
      @HttpParameter("password") String password) {
    checkString(username);
    checkString(password);
    User user;
    dalServices.openConnection();
    try {
      user = (User) userDao.findBy(UserDao.COLUMN_USERNAME, username);
    } finally {
      dalServices.closeConnection();
    }
    if (user == null) {
      logger.info("User not found in database");
      throw new UnauthenticatedUserException();
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
      logger.info("Wrong password");
      throw new UnauthenticatedUserException();
    }
    return user;
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET)
  public UserDto showAuthenticatedUser(@SessionParameter(USER_ID) int id) {
    dalServices.openConnection();
    try {
      return userDao.findById(id);
    } finally {
      dalServices.closeConnection();
    }
  }

  @Override
  @Session(action = Session.DELETE)
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.DELETE)
  public void signout() {//TODO Check if token validity issues doesn't come from there
  }

}
