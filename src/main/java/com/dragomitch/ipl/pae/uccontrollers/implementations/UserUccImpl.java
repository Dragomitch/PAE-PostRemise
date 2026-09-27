package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User accounts. The format of the data is checked by the constraints of {@link UserDto} (method
 * validation of {@link UserUcc}); this class checks the rules that need the database: unique
 * username and email, existing option.
 */
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
    checkBusinessRules(user);
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
    UserDto user;
    if ((user = userDao.findById(id)) == null) {
      throw new ResourceNotFoundException();
    }
    if (user.getRole().equals(UserDto.ROLE_STUDENT)) {
      user.setRole(UserDto.ROLE_PROFESSOR);
      // optimistic locking: the DAO only updates the row if its version is still user.getVersion()
      // and throws a ConcurrentModificationException otherwise (the transaction then rolls back)
      userDao.update(user);
    }
  }

  @Override
  public UserDto edit(UserDto user, int userId, String userRole) {
    if (userRole.equals(UserDto.ROLE_STUDENT) && user.getId() != userId) {
      throw new InsufficientPermissionException();
    }
    checkBusinessRules(user);
    UserDto existingUser = userDao.findById(user.getId());
    if (existingUser == null) {
      throw new ResourceNotFoundException();
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

  /**
   * The rules the constraints cannot check: the username and the email address are not used by
   * another account, the option exists.
   */
  private void checkBusinessRules(UserDto user) {
    UserDto existingUser = userDao.findBy(UserDao.COLUMN_USERNAME, user.getUsername());
    if (existingUser != null && existingUser.getId() != user.getId()) {
      throw new BusinessException(ErrorCode.USERNAME_TAKEN, user.getUsername());
    }
    existingUser = userDao.findBy(UserDao.COLUMN_EMAIL, user.getEmail());
    if (existingUser != null && existingUser.getId() != user.getId()) {
      throw new BusinessException(ErrorCode.EMAIL_TAKEN, user.getEmail());
    }
    if (optionDao.findByCode(user.getOption().getCode()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_OPTION, user.getOption().getCode());
    }
  }
}
