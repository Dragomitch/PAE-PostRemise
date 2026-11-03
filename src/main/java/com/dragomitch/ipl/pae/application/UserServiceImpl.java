package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User accounts. The format of the data is checked by the constraints of {@link UserDto} (method
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
  public UserDto signup(UserDto user) {
    checkBusinessRules(user);
    user.setRegistrationDate(LocalDateTime.now(clock));
    // encrypt password
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    // first user is automatically considered a professor
    if (userRepository.isEmpty()) {
      user.setRole(User.ROLE_PROFESSOR);
    } else {
      user.setRole(User.ROLE_STUDENT);
    }
    return userRepository.create(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDto> showAll() {
    return userRepository.findAll();
  }

  @Override
  public void promoteToProfessor(int id) {
    UserDto user;
    if ((user = userRepository.findById(id)) == null) {
      throw new ResourceNotFoundException();
    }
    if (user.getRole().equals(UserDto.ROLE_STUDENT)) {
      // the id is what the route gives: promote by id, with the version just read. Only the role
      // and the version are written; a stale version throws a ConcurrentModificationException
      // (the transaction then rolls back). A professor is left untouched.
      user.setVersion(userRepository.promoteToProfessor(user.getId(), user.getVersion()));
      user.setRole(UserDto.ROLE_PROFESSOR);
    }
  }

  @Override
  public UserDto promoteToProfessorByUsername(String username) {
    UserDto user = userRepository.findBy(UserRepository.COLUMN_USERNAME, username);
    if (user == null) {
      throw new ResourceNotFoundException();
    }
    if (user.getRole().equals(UserDto.ROLE_STUDENT)) {
      user.setVersion(userRepository.promoteToProfessor(username, user.getVersion()));
      user.setRole(UserDto.ROLE_PROFESSOR);
    }
    return user;
  }

  @Override
  public UserDto edit(UserDto user, int userId, String userRole) {
    if (userRole.equals(UserDto.ROLE_STUDENT) && user.getId() != userId) {
      throw new InsufficientPermissionException();
    }
    checkBusinessRules(user);
    UserDto existingUser = userRepository.findById(user.getId());
    if (existingUser == null) {
      throw new ResourceNotFoundException();
    }
    user.setPassword(existingUser.getPassword());
    user.setRegistrationDate(existingUser.getRegistrationDate());
    user.setRole(existingUser.getRole());
    userRepository.update(user);
    NominatedStudentDto nominatedStudent = nominatedStudentRepository.findById(user.getId());
    if (nominatedStudent != null && nominatedStudent.getVersion() != user.getVersion()) {
      nominatedStudentRepository.update(nominatedStudent);
    }
    return user;
  }

  /**
   * The rules the constraints cannot check: the username and the email address are not used by
   * another account, the option exists.
   */
  private void checkBusinessRules(UserDto user) {
    UserDto existingUser =
        userRepository.findBy(UserRepository.COLUMN_USERNAME, user.getUsername());
    if (existingUser != null && existingUser.getId() != user.getId()) {
      throw new BusinessException(ErrorCode.USERNAME_TAKEN, user.getUsername());
    }
    existingUser = userRepository.findBy(UserRepository.COLUMN_EMAIL, user.getEmail());
    if (existingUser != null && existingUser.getId() != user.getId()) {
      throw new BusinessException(ErrorCode.EMAIL_TAKEN, user.getEmail());
    }
    if (optionRepository.findByCode(user.getOption().getCode()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_OPTION, user.getOption().getCode());
    }
  }
}
