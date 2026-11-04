package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.InvalidCredentialsException;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.SessionService;
import com.dragomitch.ipl.pae.domain.port.out.PasswordHasher;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SessionServiceImpl implements SessionService {

  private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  SessionServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public User signin(String username, String password) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> {
              logger.info("User not found in database");
              return new InvalidCredentialsException();
            });
    if (!passwordHasher.matches(password, user.getPassword())) {
      logger.info("Wrong password");
      throw new InvalidCredentialsException();
    }
    return user;
  }

  @Override
  public User showAuthenticatedUser(int id) {
    return userRepository.findById(id).orElse(null);
  }
}
