package persistence.implementations;

import business.EntityFactory;
import business.dto.CountryDto;
import business.dto.ProgrammeDto;
import main.ContextManager;
import main.annotations.Inject;
import main.exceptions.FatalException;
import persistence.CountryDao;
import persistence.ProgrammeDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class CountryDaoImpl implements CountryDao {
  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);
  private static final String SQL_SELECT = "SELECT c." + COLUMN_CODE + ", c." + COLUMN_NAME + ", p."
      + ProgrammeDao.COLUMN_ID + ", p." + ProgrammeDao.COLUMN_NAME + ", p."
      + ProgrammeDao.COLUMN_EXTERNAL_SOFTWARE_NAME + " FROM " + SCHEMA_NAME + "." + TABLE_NAME
      + " c, " + SCHEMA_NAME + "." + ProgrammeDao.TABLE_NAME + " p WHERE c." + COLUMN_PROGRAMME_ID
      + " = p." + ProgrammeDao.COLUMN_ID;

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public CountryDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public CountryDto findById(String countryCode) {
    CountryDto country = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " AND c." + COLUMN_CODE + " = ?")) {
      stmt.setString(1, countryCode);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          country = populateCountryDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return country;
  }

  @Override
  public List<CountryDto> findAll() {
    List<CountryDto> countries = new ArrayList<CountryDto>();
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " ORDER BY c." + COLUMN_NAME + " ASC")) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          countries.add(populateCountryDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return countries;
  }

  /**
   * Populates a CountryDto object from a row of data.
   * 
   * @param rs a cursor pointing to its current row of data
   * @return country an instance of CountryDto built based on a row of data
   */
  private CountryDto populateCountryDto(ResultSet rs) throws SQLException {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(1));
    country.setName(rs.getString(2));
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(rs.getInt(3));;
    programme.setProgrammeName(rs.getString(4));
    programme.setExternalSoftName(rs.getString(5));
    country.setProgramme(programme);
    return country;
  }

}
