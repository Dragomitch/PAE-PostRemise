package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import org.springframework.stereotype.Repository;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
class CountryDaoImpl implements CountryDao {
  private static final String SCHEMA_NAME = "student_exchange_tools";
  private static final String SQL_SELECT = "SELECT c." + COLUMN_CODE + ", c." + COLUMN_NAME + ", p."
      + ProgrammeDao.COLUMN_ID + ", p." + ProgrammeDao.COLUMN_NAME + ", p."
      + ProgrammeDao.COLUMN_EXTERNAL_SOFTWARE_NAME + " FROM " + SCHEMA_NAME + "." + TABLE_NAME
      + " c, " + SCHEMA_NAME + "." + ProgrammeDao.TABLE_NAME + " p WHERE c." + COLUMN_PROGRAMME_ID
      + " = p." + ProgrammeDao.COLUMN_ID;

  private final DtoFactory dtoFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param dtoFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  
  public CountryDaoImpl(DtoFactory dtoFactory, DalBackendServices dalBackendServices) {
    this.dtoFactory = dtoFactory;
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
    CountryDto country = (CountryDto) dtoFactory.create(CountryDto.class);
    country.setCountryCode(rs.getString(1));
    country.setName(rs.getString(2));
    ProgrammeDto programme = (ProgrammeDto) dtoFactory.create(ProgrammeDto.class);
    programme.setId(rs.getInt(3));;
    programme.setProgrammeName(rs.getString(4));
    programme.setExternalSoftName(rs.getString(5));
    country.setProgramme(programme);
    return country;
  }

}
