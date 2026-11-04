package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User accounts. The format of the data is checked by the constraints of {@link User} (method
 * validation of {@link UserService}); this class checks the rules that need the database: unique
 * username and email, existing option.
 */
@Service
@Transactional
class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final OptionRepository optionRepository;
  private final NominatedStudentRepository nominatedStudentRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  UserServiceImpl(
      UserRepository userRepository,
      OptionRepository optionRepository,
      NominatedStudentRepository nominatedStudentRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.userRepository = userRepository;
    this.optionRepository = optionRepository;
    this.nominatedStudentRepository = nominatedStudentRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  public User signup(User user) {
    checkBusinessRules(user);
    user.setRegistrationDate(LocalDateTime.now(clock));
    // encrypt password
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    // first user is automatically considered a professor
    if (userRepository.isEmpty()) {
      user.setRole(Role.PROFESSOR);
    } else {
      user.setRole(Role.STUDENT);
    }
    return userRepository.create(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> showAll() {
    return userRepository.findAll();
  }

  @Override
  public void promoteToProfessor(int id) {
    User user = userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    if (user.getRole() == Role.STUDENT) {
      // the id is what the route gives: promote by id, with the version just read. Only the role
      // and the version are written; a stale version throws a ConcurrentModificationException
      // (the transaction then rolls back). A professor is left untouched.
      user.setVersion(userRepository.promoteToProfessor(user.getId(), user.getVersion()));
      user.setRole(Role.PROFESSOR);
    }
  }

  @Override
  public User promoteToProfessorByUsername(String username) {
    User user = userRepository.findByUsername(username).orElseThrow(ResourceNotFoundException::new);
    if (user.getRole() == Role.STUDENT) {
      user.setVersion(userRepository.promoteToProfessor(username, user.getVersion()));
      user.setRole(Role.PROFESSOR);
    }
    return user;
  }

  @Override
  public User edit(User user, int userId, Role userRole) {
    if (userRole == Role.STUDENT && user.getId() != userId) {
      throw new InsufficientPermissionException();
    }
    checkBusinessRules(user);
    User existingUser =
        userRepository.findById(user.getId()).orElseThrow(ResourceNotFoundException::new);
    user.setPassword(existingUser.getPassword());
    user.setRegistrationDate(existingUser.getRegistrationDate());
    user.setRole(existingUser.getRole());
    userRepository.update(user);
    nominatedStudentRepository
        .findById(user.getId())
        .filter(student -> student.getVersion() != user.getVersion())
        .ifPresent(nominatedStudentRepository::update);
    return user;
  }

  /**
   * The rules the constraints cannot check: the username and the email address are not used by
   * another account, the option exists.
   */
  private void checkBusinessRules(User user) {
    if (isAnotherUser(userRepository.findByUsername(user.getUsername()), user)) {
      throw new BusinessException(ErrorCode.USERNAME_TAKEN, user.getUsername());
    }
    if (isAnotherUser(userRepository.findByEmail(user.getEmail()), user)) {
      throw new BusinessException(ErrorCode.EMAIL_TAKEN, user.getEmail());
    }
    if (optionRepository.findByCode(user.getOption().getCode()).isEmpty()) {
      throw new BusinessException(ErrorCode.UNKNOWN_OPTION, user.getOption().getCode());
    }
  }

  private static boolean isAnotherUser(Optional<User> found, User user) {
    return found.filter(existing -> existing.getId() != user.getId()).isPresent();
  }
}
