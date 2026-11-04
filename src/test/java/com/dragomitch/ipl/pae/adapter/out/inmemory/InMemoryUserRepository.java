package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository, ResettableRepository {

  private List<User> users;

  public InMemoryUserRepository() {
    users = new ArrayList<User>();
  }

  @Override
  public User create(User user) {
    users.add(user);
    user.setId(users.size());
    return user;
  }

  @Override
  public Optional<User> findById(int id) {
    if (id > 0 && id <= users.size()) {
      return Optional.ofNullable(users.get((id - 1)));
    }
    return Optional.empty();
  }

  @Override
  public List<User> findAll() {
    return users;
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return users.stream().filter(user -> user.getUsername().equals(username)).findFirst();
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return users.stream().filter(user -> user.getEmail().equals(email)).findFirst();
  }

  @Override
  public int promoteToProfessor(int userId, int expectedVersion) {
    return promote(findById(userId).orElse(null), expectedVersion);
  }

  @Override
  public int promoteToProfessor(String username, int expectedVersion) {
    return promote(findByUsername(username).orElse(null), expectedVersion);
  }

  private int promote(User user, int expectedVersion) {
    if (user == null || user.getVersion() != expectedVersion) {
      throw new ConcurrentModificationException();
    }
    user.setRole(Role.PROFESSOR);
    user.setVersion(expectedVersion + 1);
    return expectedVersion + 1;
  }

  @Override
  public void update(User user) {
    user.setVersion(users.get(user.getId() - 1).getVersion() + 1);
    users.set(user.getId() - 1, user);
  }

  @Override
  public boolean isEmpty() {
    return users.isEmpty();
  }

  @Override
  public void empty() {
    users = new ArrayList<User>();
  }
}
