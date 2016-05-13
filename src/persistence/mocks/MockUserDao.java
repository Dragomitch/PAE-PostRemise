package persistence.mocks;

import business.dto.UserDto;
import persistence.UserDao;

import java.util.ArrayList;
import java.util.List;

public class MockUserDao implements UserDao {

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
  public void promoteToProfessor(int id) {
    if (id > 0 || id <= users.size()) {
      UserDto user = null;
      user = users.get(id);
      user.setRole(UserDto.ROLE_PROFESSOR);
    }
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

  public void empty() {
    users = new ArrayList<UserDto>();
  }

}
