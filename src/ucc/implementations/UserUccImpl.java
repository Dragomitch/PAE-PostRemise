package ucc.implementations;

import static utils.DataValidationUtils.checkObject;
import static utils.DataValidationUtils.checkPositive;
import static utils.DataValidationUtils.checkString;

import business.User;
import business.dto.NominatedStudentDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import business.exceptions.ErrorFormat;
import business.exceptions.RessourceNotFoundException;
import main.annotations.Inject;
import persistence.NominatedStudentDao;
import persistence.OptionDao;
import persistence.UserDao;
import presentation.annotations.ApiCollection;
import presentation.annotations.HttpParameter;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.annotations.SessionParameter;
import presentation.enums.HttpMethod;
import ucc.UnitOfWork;
import ucc.UserUcc;
import utils.DataValidationUtils;

import org.mindrot.BCrypt;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@ApiCollection(name = "Users", endpoint = "/users")
class UserUccImpl implements UserUcc {

  private UserDao userDao;
  private OptionDao optionDao;
  private NominatedStudentDao nominatedStudentDao;
  private UnitOfWork unitOfWork;

  @Inject
  public UserUccImpl(UserDao userDao, OptionDao optionDao, NominatedStudentDao nominatedStudentDao,
      UnitOfWork unitOfWork) {
    this.userDao = userDao;
    this.optionDao = optionDao;
    this.nominatedStudentDao = nominatedStudentDao;
    this.unitOfWork = unitOfWork;
  }

  @Override
  @Route(method = HttpMethod.POST)
  public UserDto signup(@HttpParameter("data") UserDto user) {
    checkObject(user);
    try {
      unitOfWork.startTransaction();
      checkDataIntegrity(user);
      user.setRegistrationDate(LocalDateTime.now());
      // encrypt password
      user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
      // first user is automatically considered a professor
      if (userDao.isEmpty()) {
        user.setRole(User.ROLE_PROFESSOR);
      } else {
        user.setRole(User.ROLE_STUDENT);
      }
      UserDto newUser = userDao.create(user);
      unitOfWork.commit();
      return newUser;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET)
  public Map<String, Object> showAll() {
    try {
      unitOfWork.startTransaction();
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("data", userDao.findAll());
      unitOfWork.commit();
      return data;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/{id}/promote")
  public void promoteToProfessor(@PathParameter("id") int id) {
    DataValidationUtils.checkPositiveOrZero(id);
    UserDto user;
    try {
      unitOfWork.startTransaction();
      if ((user = userDao.findById(id)) == null) {
        throw new RessourceNotFoundException();
      }
      if (user.getRole().equals(UserDto.ROLE_STUDENT)) {
        user.setRole(UserDto.ROLE_PROFESSOR);
        unitOfWork.update(user);
      }
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.PUT, template = "/edit")
  public UserDto edit(@HttpParameter("data") UserDto user, @SessionParameter("userId") int userId,
      @SessionParameter("userRole") String userRole) {
    checkObject(user);
    checkPositive(userId);
    checkString(userRole);
    try {
      unitOfWork.startTransaction();
      checkDataIntegrity(user);
      UserDto existingUser = userDao.findById(user.getId());
      if (existingUser == null) {
        throw new RessourceNotFoundException();
      }
      user.setPassword(existingUser.getPassword());
      user.setRegistrationDate(existingUser.getRegistrationDate());
      user.setRole(existingUser.getRole());
      userDao.update(user);
      NominatedStudentDto nominatedStudent = nominatedStudentDao.findById(user.getId());
      if (nominatedStudent != null && nominatedStudent.getVersion() != user.getVersion()) {
        nominatedStudentDao.update(nominatedStudent);
      }
      unitOfWork.commit();
      return user;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  private void checkDataIntegrity(UserDto user) {
    List<Integer> violations = new LinkedList<Integer>();
    try {
      ((User) user).checkDataIntegrity();
    } catch (BusinessException ex) {
      List<ErrorFormat> errors = ex.getError().getDetails();
      for (ErrorFormat oneError : errors) {
        violations.add(oneError.getErrorCode());
      }
    }
    UserDto existingUser = userDao.findBy(UserDao.COLUMN_USERNAME, user.getUsername());
    if (existingUser != null && existingUser.getId() != user.getId()) {
      violations.add(ErrorFormat.UNICITY_VIOLATION_USERNAME_204);
    }
    existingUser = userDao.findBy(UserDao.COLUMN_EMAIL, user.getEmail());
    if (existingUser != null && existingUser.getId() != user.getId()) {
      violations.add(ErrorFormat.UNICITY_VIOLATION_EMAIL_207);
    }
    if (user.getOption().getCode().length() != OptionDao.OPTION_CODE_LENGTH) {
      violations.add(ErrorFormat.INVALID_OPTION_CODE_LENGTH_216);
    } else if (optionDao.findByCode(user.getOption().getCode()) == null) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_OPTION_210);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
