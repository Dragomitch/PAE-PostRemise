package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ConcurrentModificationException;

class AddressDaoImpl implements AddressDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_SELECT = "SELECT a." + COLUMN_ID + ", a." + COLUMN_STREET + ", a."
      + COLUMN_NUMBER + ", a." + COLUMN_COUNTRY + ", c." + CountryDao.COLUMN_NAME + ", a."
      + COLUMN_CITY + ", a." + COLUMN_POSTAL_CODE + ", a." + COLUMN_REGION + ", a.version "
      + "FROM " + SCHEMA_NAME + "." + CountryDao.TABLE_NAME + " c, " + SCHEMA_NAME + "."
      + TABLE_NAME + " a " + "WHERE a." + COLUMN_COUNTRY + " = c." + CountryDao.COLUMN_CODE;

  private static final String SQL_INSERT =
      "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + "(" + COLUMN_STREET + ", " + COLUMN_NUMBER
          + ", " + COLUMN_COUNTRY + ", " + COLUMN_CITY + ", " + COLUMN_POSTAL_CODE + ", "
          + COLUMN_REGION + ", version) VALUES (?, ?, ?, ?, ?, ?, 1)" + " RETURNING " + COLUMN_ID;

  private static final String SQL_UPDATE = "UPDATE " + SCHEMA_NAME + "." + TABLE_NAME + " a SET ( "
      + COLUMN_STREET + ", " + COLUMN_NUMBER + ", " + COLUMN_COUNTRY + ", " + COLUMN_CITY + ", "
      + COLUMN_POSTAL_CODE + ", " + COLUMN_REGION + ", version ) = (?,?,?,?,?,?,version+1) "
      + "WHERE a." + COLUMN_ID + " = ? AND a.version= ? RETURNING a.version";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser.
   * @param dalBackendServices backend services.
   */
  @Inject
  public AddressDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public AddressDto create(AddressDto address) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_INSERT)) {
      populatePreparedStatement(stmt, address, SQL_INSERT);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        address.setId(rs.getInt(1));
      }
      address.setVersion(1);
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return address;
  }

  @Override
  public AddressDto findById(int id) {
    AddressDto address = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " AND a." + COLUMN_ID + " = ?")) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          address = populateAddressDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return address;
  }

  @Override
  public AddressDto update(AddressDto address) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_UPDATE)) {
      populatePreparedStatement(stmt, address, SQL_UPDATE);
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new ConcurrentModificationException();
        }
        address.setVersion(rs.getInt(1));
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return address;
  }

  /**
   * Populates an AddressDto object from a row of data.
   * 
   * @param rs a cursor pointing to its current row of data
   * @return address an instance of AddressDto built based on a row of data
   */
  private AddressDto populateAddressDto(ResultSet rs) throws SQLException {
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(rs.getInt(1));
    address.setStreet(rs.getString(2));
    address.setNumber(rs.getString(3));
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(4));
    country.setName(rs.getString(5));
    address.setCountry(country);
    address.setCity(rs.getString(6));
    address.setPostalCode(rs.getString(7));
    address.setRegion(rs.getString(8));
    address.setVersion(rs.getInt(9));
    return address;
  }

  /**
   * Set the statement for a SQL_CREATE and SQL_UPDATE query.
   * 
   * @param stmt the PreparedStatement to set properly.
   * @param address the providing the data.
   * @param query the query for which we are populating the PreparedStatement.
   * @return the statement modified ( not a new object ).
   */
  private void populatePreparedStatement(PreparedStatement stmt, AddressDto address, String query) {
    try {
      stmt.setString(1, address.getStreet());
      stmt.setString(2, address.getNumber());
      stmt.setString(3, address.getCountry().getCountryCode());
      stmt.setString(4, address.getCity());
      stmt.setString(5, address.getPostalCode());
      if (address.getRegion() == null) {
        stmt.setNull(6, Types.VARCHAR);
      } else {
        stmt.setString(6, address.getRegion());
      }
      if (query.equals(SQL_UPDATE)) {
        stmt.setInt(7, address.getId());
        stmt.setInt(8, address.getVersion());
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }
}
