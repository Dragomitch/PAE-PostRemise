package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class MockUserDao implements UserDao, ResettableMock {

  private List<UserDto> users;

  public MockUserDao() {
    users = new ArrayList<UserDto>();
  }

  @Override
  public UserDto create(UserDto user) {
    users.add(user);
    user.setId(users.size());
    return user;
  }

  @Override
  public UserDto findById(int id) {
    if (id > 0 && id <= users.size()) {
      return users.get((id - 1));
    }
    return null;
  }

  @Override
  public List<UserDto> findAll() {
    return users;
  }

  @Override
  public UserDto findBy(String columnName, String columnValue) {
    for (int i = 0; i < users.size(); i++) {
      if (columnName.equals(COLUMN_USERNAME)) {
        if (users.get(i).getUsername().equals(columnValue)) {
          return users.get(i);
        }
      }
      if (columnName.equals(COLUMN_EMAIL)) {
        if (users.get(i).getEmail().equals(columnValue)) {
          return users.get(i);
        }
      }
    }
    return null;
  }

  @Override
  public int promoteToProfessor(int userId, int expectedVersion) {
    return promote(findById(userId), expectedVersion);
  }

  @Override
  public int promoteToProfessor(String username, int expectedVersion) {
    return promote(findBy(COLUMN_USERNAME, username), expectedVersion);
  }

  private int promote(UserDto user, int expectedVersion) {
    if (user == null || user.getVersion() != expectedVersion) {
      throw new ConcurrentModificationException();
    }
    user.setRole(UserDto.ROLE_PROFESSOR);
    user.setVersion(expectedVersion + 1);
    return expectedVersion + 1;
  }

  @Override
  public void update(UserDto user) {
    user.setVersion(users.get(user.getId() - 1).getVersion() + 1);
    users.set(user.getId() - 1, user);
  }

  @Override
  public boolean isEmpty() {
    return users.isEmpty();
  }

  @Override
  public void empty() {
    users = new ArrayList<UserDto>();
  }

}
