package ucc.implementations;

import static utils.DataValidationUtils.checkString;

import business.User;
import business.dto.UserDto;
import main.annotations.Inject;
import main.logging.LogManager;
import persistence.DalServices;
import persistence.UserDao;
import presentation.annotations.ApiCollection;
import presentation.annotations.HttpParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.annotations.Session;
import presentation.annotations.SessionParameter;
import presentation.enums.HttpMethod;
import presentation.exceptions.UnauthenticatedUserException;
import ucc.SessionUcc;

import org.mindrot.BCrypt;

import java.util.logging.Logger;

@ApiCollection(name = "Session", endpoint = "/session")
class SessionUccImpl implements SessionUcc {

  private static Logger logger = LogManager.getLogger(SessionUccImpl.class.getName());

  private UserDao userDao;
  private DalServices dalServices;

  @Inject
  public SessionUccImpl(UserDao userDao, DalServices dalServices) {
    this.userDao = userDao;
    this.dalServices = dalServices;
  }

  @Override
  @Session(action = Session.SET, attributeNames = {USER_ID, USER_ROLE}, fieldNames = {"id", "role"})
  @Route(method = HttpMethod.POST)
  public UserDto signin(@HttpParameter("username") String username,
      @HttpParameter("password") String password) {
    checkString(username);
    checkString(password);
    dalServices.openConnection();
    User user = (User) userDao.findBy(UserDao.COLUMN_USERNAME, username);
    if (user == null) {
      logger.info("User not found in database");
      throw new UnauthenticatedUserException();
    }
    if (!BCrypt.checkpw(password, user.getPassword())) {
      logger.info("Wrong password");
      throw new UnauthenticatedUserException();
    }
    dalServices.closeConnection();
    return user;
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET)
  public UserDto showAuthenticatedUser(@SessionParameter(USER_ID) int id) {
    dalServices.openConnection();
    UserDto currentUser = userDao.findById(id);
    dalServices.closeConnection();
    return currentUser;
  }

  @Override
  @Session(action = Session.DELETE)
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.DELETE)
  public void signout() {}

}
