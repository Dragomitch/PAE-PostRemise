package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.InvalidCredentialsException;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SessionUccImpl implements SessionUcc {

  private static final Logger logger = LoggerFactory.getLogger(SessionUccImpl.class);

  private final UserDao userDao;
  private final PasswordEncoder passwordEncoder;

  SessionUccImpl(UserDao userDao, PasswordEncoder passwordEncoder) {
    this.userDao = userDao;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDto signin(String username, String password) {
    User user = (User) userDao.findBy(UserDao.COLUMN_USERNAME, username);
    if (user == null) {
      logger.info("User not found in database");
      throw new InvalidCredentialsException();
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
      logger.info("Wrong password");
      throw new InvalidCredentialsException();
    }
    return user;
  }

  @Override
  public UserDto showAuthenticatedUser(int id) {
    return userDao.findById(id);
  }

}
