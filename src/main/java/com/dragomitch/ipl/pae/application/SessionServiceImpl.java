package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.InvalidCredentialsException;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.SessionService;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SessionServiceImpl implements SessionService {

  private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  SessionServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public User signin(String username, String password) {
    User user = (User) userRepository.findBy(UserRepository.COLUMN_USERNAME, username);
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
  public User showAuthenticatedUser(int id) {
    return userRepository.findById(id);
  }
}
