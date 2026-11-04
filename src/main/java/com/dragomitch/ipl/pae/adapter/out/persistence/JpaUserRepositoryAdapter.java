package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.OptionEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.UserJpaRepository;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** The users of the application: students and professors. */
@Repository
class JpaUserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository users;
  private final PersistenceMapper mapper;

  @PersistenceContext private EntityManager entityManager;

  JpaUserRepositoryAdapter(UserJpaRepository users, PersistenceMapper mapper) {
    this.users = users;
    this.mapper = mapper;
  }

  /** Stores the user at version 1 and sets its generated id and version on the DTO. */
  @Override
  public User create(User user) {
    UserEntity entity = new UserEntity();
    copy(user, entity);
    entity.setRegistrationDate(user.getRegistrationDate());
    entity.setVersion(1);
    entityManager.persist(entity);
    user.setId(entity.getId());
    user.setVersion(entity.getVersion());
    return user;
  }

  @Override
  public Optional<User> findById(int id) {
    return users.findWithOptionById(id).map(mapper::user);
  }

  @Override
  public List<User> findAll() {
    return users.findAllWithOptionByOrderById().stream().map(mapper::user).toList();
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return users.findFirstWithOptionByUsernameOrderById(username).map(mapper::user);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return users.findFirstWithOptionByEmailOrderById(email).map(mapper::user);
  }

  @Override
  public int promoteToProfessor(int userId, int expectedVersion) {
    return promoted(
        users.updateRoleById(userId, Role.PROFESSOR.label(), expectedVersion),
        userId,
        expectedVersion);
  }

  @Override
  public int promoteToProfessor(String username, int expectedVersion) {
    return promoted(
        users.updateRoleByUsername(username, Role.PROFESSOR.label(), expectedVersion),
        username,
        expectedVersion);
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the user does not exist or its version is not the
   *     DTO's (it was changed since it was read)
   */
  @Override
  public void update(User user) {
    UserEntity entity =
        users
            .findById(user.getId())
            .filter(found -> found.getVersion() == user.getVersion())
            .orElseThrow(ConcurrentModificationException::new);
    copy(user, entity);
    Versions.flushIncrementingVersion(entityManager, entity, entity::getVersion, user.getVersion());
    user.setVersion(entity.getVersion());
  }

  @Override
  public boolean isEmpty() {
    return users.count() == 0;
  }

  /** The new version after a promotion: 0 users changed means an unknown user or a stale one. */
  private static int promoted(int changed, Object user, int expectedVersion) {
    if (changed == 0) {
      throw new ConcurrentModificationException(
          "User " + user + " with version " + expectedVersion + " not found");
    }
    return expectedVersion + 1;
  }

  /** The columns a create and an update write (the registration date is set once, on create). */
  private void copy(User user, UserEntity entity) {
    entity.setUsername(user.getUsername());
    entity.setLastName(user.getLastName());
    entity.setFirstName(user.getFirstName());
    entity.setEmail(user.getEmail());
    entity.setPassword(user.getPassword());
    entity.setRole(user.getRole() == null ? null : user.getRole().label());
    entity.setOption(entityManager.getReference(OptionEntity.class, user.getOption().getCode()));
  }
}
