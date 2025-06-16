package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import org.springframework.stereotype.Repository;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

@Repository
class UserDaoImpl implements UserDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String CREATE_QUERY = "INSERT INTO " + SCHEMA_NAME + "." + UserDao.TABLE_NAME
      + "(" + UserDao.COLUMN_USERNAME + ", " + UserDao.COLUMN_LAST_NAME + ", "
      + UserDao.COLUMN_FIRST_NAME + ", " + UserDao.COLUMN_EMAIL + ", " + UserDao.COLUMN_PASSWORD
      + ", " + UserDao.COLUMN_OPTION + ", " + UserDao.COLUMN_ROLE + ", "
      + UserDao.COLUMN_REGISTRATION_DATE + ", " + UserDao.COLUMN_VERSION + ") "
      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1) RETURNING " + UserDao.COLUMN_ID;

  private static final String UPDATE = "UPDATE " + SCHEMA_NAME + "." + UserDao.TABLE_NAME + " SET ("
      + UserDao.COLUMN_USERNAME + ", " + UserDao.COLUMN_LAST_NAME + ", " + UserDao.COLUMN_FIRST_NAME
      + ", " + UserDao.COLUMN_EMAIL + ", " + UserDao.COLUMN_PASSWORD + ", " + UserDao.COLUMN_OPTION
      + ", " + UserDao.COLUMN_ROLE + ", " + UserDao.COLUMN_VERSION + ") "
      + "= (?, ?, ?, ?, ?, ?, ?, version + 1) WHERE " + UserDao.COLUMN_ID + " = ? AND "
      + UserDao.COLUMN_VERSION + " = ? RETURNING " + UserDao.COLUMN_VERSION;

  private static final String FIND_QUERY = "SELECT u." + UserDao.COLUMN_ID + ", u."
      + UserDao.COLUMN_LAST_NAME + ", u." + UserDao.COLUMN_FIRST_NAME + ", u."
      + UserDao.COLUMN_USERNAME + ", u." + UserDao.COLUMN_PASSWORD + ", u." + UserDao.COLUMN_EMAIL
      + "," + "u." + UserDao.COLUMN_REGISTRATION_DATE + ", u." + UserDao.COLUMN_ROLE + ", u."
      + UserDao.COLUMN_VERSION + ", op." + OptionDao.COLUMN_CODE + ", op." + OptionDao.COLUMN_NAME
      + " FROM " + SCHEMA_NAME + "." + UserDao.TABLE_NAME + " u, " + SCHEMA_NAME + "."
      + OptionDao.TABLE_NAME + " op " + "WHERE op." + OptionDao.COLUMN_CODE + " = u."
      + UserDao.COLUMN_OPTION;

  private static final String PROMOTE_QUERY =
      "UPDATE student_exchange_tools.users SET role = ? WHERE username = ?";

  private static final String IS_EMPTY_QUERY = "SELECT 1 FROM student_exchange_tools.users";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public UserDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public UserDto create(UserDto user) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(CREATE_QUERY)) {
      populatePreparedStatement(stmt, user, 1);
      stmt.setTimestamp(8, Timestamp.valueOf(user.getRegistrationDate()));
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          user.setId(rs.getInt(1));
        }
      }
      user.setVersion(1);
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return user;
  }

  private void populatePreparedStatement(PreparedStatement ps, UserDto user, int parameterIndex)
      throws SQLException {
    ps.setString(parameterIndex++, user.getUsername());
    ps.setString(parameterIndex++, user.getLastName());
    ps.setString(parameterIndex++, user.getFirstName());
    ps.setString(parameterIndex++, user.getEmail());
    ps.setString(parameterIndex++, user.getPassword());
    ps.setString(parameterIndex++, user.getOption().getCode());
    ps.setString(parameterIndex++, user.getRole());
  }

  @Override
  public UserDto findById(int id) {
    UserDto user = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(FIND_QUERY + " AND u." + UserDao.COLUMN_ID + " = ?")) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          user = populateUserDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return user;
  }

  @Override
  public List<UserDto> findAll() {
    List<UserDto> users = new ArrayList<UserDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(FIND_QUERY)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          users.add(populateUserDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return users;
  }

  @Override
  public void update(UserDto user) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(UPDATE)) {
      populatePreparedStatement(stmt, user, 1);
      stmt.setInt(8, user.getId());
      stmt.setInt(9, user.getVersion());
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new ConcurrentModificationException();
        }
        user.setVersion(rs.getInt(1));
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }

  @Override
  public UserDto findBy(String columnName, String columnValue) {
    UserDto user = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(FIND_QUERY + " AND ? = u." + columnName)) {
      stmt.setString(1, columnValue);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          user = populateUserDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return user;
  }

  @Override
  public void promoteToProfessor(int id) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(PROMOTE_QUERY)) {
      stmt.setString(1, UserDto.ROLE_PROFESSOR);
      stmt.setInt(2, id);
      stmt.execute();
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }


  @Override
  public boolean isEmpty() {
    boolean isEmpty = false;
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(IS_EMPTY_QUERY)) {
      try (ResultSet rs = stmt.executeQuery()) {
        isEmpty = !rs.next();
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return isEmpty;
  }

  private UserDto populateUserDto(ResultSet rs) throws SQLException {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(rs.getInt(1));
    user.setLastName(rs.getString(2));
    user.setFirstName(rs.getString(3));
    user.setUsername(rs.getString(4));
    user.setPassword(rs.getString(5));
    user.setEmail(rs.getString(6));
    user.setRegistrationDate(rs.getTimestamp(7).toLocalDateTime());
    user.setRole(rs.getString(8));
    user.setVersion(rs.getInt(9));
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(rs.getString(10));
    option.setName(rs.getString(11));
    user.setOption(option);
    return user;
  }
}
