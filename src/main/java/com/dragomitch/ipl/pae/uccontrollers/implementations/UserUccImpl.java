package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositive;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;
import com.dragomitch.ipl.pae.utils.DataValidationUtils;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class UserUccImpl implements UserUcc {

  private final UserDao userDao;
  private final OptionDao optionDao;
  private final NominatedStudentDao nominatedStudentDao;
  private final PasswordEncoder passwordEncoder;

  UserUccImpl(UserDao userDao, OptionDao optionDao, NominatedStudentDao nominatedStudentDao,
      PasswordEncoder passwordEncoder) {
    this.userDao = userDao;
    this.optionDao = optionDao;
    this.nominatedStudentDao = nominatedStudentDao;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDto signup(UserDto user) {
    checkObject(user);
    checkDataIntegrity(user);
    user.setRegistrationDate(LocalDateTime.now());
    // encrypt password
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    // first user is automatically considered a professor
    if (userDao.isEmpty()) {
      user.setRole(User.ROLE_PROFESSOR);
    } else {
      user.setRole(User.ROLE_STUDENT);
    }
    return userDao.create(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> showAll() {
    return userDao.findAll();
  }

  @Override
  public void promoteToProfessor(int id) {
    DataValidationUtils.checkPositiveOrZero(id);
    UserDto user;
    if ((user = userDao.findById(id)) == null) {
      throw new RessourceNotFoundException();
    }
    if (user.getRole().equals(UserDto.ROLE_STUDENT)) {
      // the id is what the route gives: promote by id, with the version just read. Only the role
      // and the version are written; a stale version throws a ConcurrentModificationException
      // (the transaction then rolls back). A professor is left untouched.
      user.setVersion(userDao.promoteToProfessor(user.getId(), user.getVersion()));
      user.setRole(UserDto.ROLE_PROFESSOR);
    }
  }

  @Override
  public UserDto promoteToProfessorByUsername(String username) {
    DataValidationUtils.checkString(username);
    UserDto user = userDao.findBy(UserDao.COLUMN_USERNAME, username);
    if (user == null) {
      throw new RessourceNotFoundException();
    }
    if (user.getRole().equals(UserDto.ROLE_STUDENT)) {
      user.setVersion(userDao.promoteToProfessor(username, user.getVersion()));
      user.setRole(UserDto.ROLE_PROFESSOR);
    }
    return user;
  }

  @Override
  public UserDto edit(UserDto user, int userId, String userRole) {
    checkObject(user);
    checkPositive(userId);
    checkString(userRole);
    if (userRole.equals(UserDto.ROLE_STUDENT) && user.getId() != userId) {
      throw new InsufficientPermissionException("A student can only edit his own account");
    }
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
    return user;
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
