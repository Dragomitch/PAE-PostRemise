package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

class PartnerDaoImpl implements PartnerDao {

  private static final String SCHEMA = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_INSERT = "INSERT INTO " + SCHEMA + "." + TABLE_NAME + " ("
      + COLUMN_LEGAL_NAME + ", " + COLUMN_BUSINESS_NAME + ", " + COLUMN_FULL_NAME + ", "
      + COLUMN_ORGANISATION_TYPE + ", " + COLUMN_EMPLOYEE_COUNT + ", " + COLUMN_ADDRESS + ", "
      + COLUMN_EMAIL + ", " + COLUMN_WEBSITE + ", " + COLUMN_PHONE_NUMBER + ", "
      + COLUMN_STATUS_OFFICIAL + ", " + COLUMN_ARCHIVE + ", version) "
      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, 1) RETURNING partner_id";

  private static final String SQL_SELECT = "SELECT DISTINCT p." + COLUMN_ID + ", p."
      + COLUMN_LEGAL_NAME + ", p." + COLUMN_BUSINESS_NAME + ", p." + COLUMN_FULL_NAME + ", p."
      + COLUMN_ORGANISATION_TYPE + ", p." + COLUMN_EMPLOYEE_COUNT + ", p." + COLUMN_ADDRESS + ", p."
      + COLUMN_EMAIL + ", p." + COLUMN_WEBSITE + ", p." + COLUMN_PHONE_NUMBER + ", p."
      + COLUMN_STATUS_OFFICIAL + ", p." + COLUMN_ARCHIVE + ", p." + COLUMN_VERSION
      + ", pr.programme_id, pr.name," + " c.country_code, c.name FROM " + SCHEMA + "." + TABLE_NAME
      + " p, " + SCHEMA + "." + AddressDao.TABLE_NAME + " a, " + SCHEMA + "."
      + PartnerOptionDao.TABLE_NAME + " po, " + SCHEMA + "." + OptionDao.TABLE_NAME + " o, "
      + SCHEMA + "." + CountryDao.TABLE_NAME + " c, " + SCHEMA + "." + ProgrammeDao.TABLE_NAME
      + " pr " + "WHERE p." + COLUMN_ADDRESS + " = a." + AddressDao.COLUMN_ID + " AND a."
      + AddressDao.COLUMN_COUNTRY + " = c." + CountryDao.COLUMN_CODE + " " + "AND c."
      + CountryDao.COLUMN_PROGRAMME_ID + " = pr." + ProgrammeDao.COLUMN_ID
      + " AND po.partner_id = p.partner_id " + "AND po.option_code = o.option_code";

  private static final String SQL_UPDATE = "UPDATE " + SCHEMA + "." + TABLE_NAME + " p SET ("
      + COLUMN_LEGAL_NAME + ", " + COLUMN_BUSINESS_NAME + ", " + COLUMN_FULL_NAME + ", "
      + COLUMN_ORGANISATION_TYPE + ", " + COLUMN_EMPLOYEE_COUNT + ", " + COLUMN_ADDRESS + ", "
      + COLUMN_EMAIL + ", " + COLUMN_WEBSITE + ", " + COLUMN_PHONE_NUMBER + ", "
      + COLUMN_STATUS_OFFICIAL + ", " + COLUMN_ARCHIVE + ", " + COLUMN_VERSION
      + ") = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version+1) WHERE p." + COLUMN_ID + " = ? AND p."
      + COLUMN_VERSION + " = ? RETURNING p." + COLUMN_VERSION;


  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  @Inject
  public PartnerDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public PartnerDto create(PartnerDto partner) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_INSERT)) {
      populatePreparedStatement(stmt, partner, SQL_INSERT);
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        partner.setId(rs.getInt(1));
      }
      partner.setVersion(1);
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return partner;
  }

  @Override
  public PartnerDto findById(int id) {
    PartnerDto partner = null;
    try {
      PreparedStatement stmt =
          dalBackendServices.prepareStatement(SQL_SELECT + " AND p." + COLUMN_ID + " = ?");
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          partner = populatePartnerDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return partner;
  }

  @Override
  public List<PartnerDto> findAll(String filter, String value, String userRole, String option) {
    List<PartnerDto> partners = new ArrayList<PartnerDto>();
    String queryFilter = "";
    if (filter.equals(FILTER_ALL_PARTNERS)) {
      if (userRole.equals(UserDto.ROLE_STUDENT)) {
        queryFilter += " AND p." + COLUMN_ARCHIVE + "= FALSE AND p." + COLUMN_STATUS_OFFICIAL
            + "= TRUE AND po." + PartnerOptionDao.COLUMN_OPTION_CODE + "=?";
      }
    } else if (filter.equals(FILTER_COUNTRY)) {
      queryFilter += " AND c." + CountryDao.COLUMN_CODE + " = ?";
      if (userRole.equals(UserDto.ROLE_STUDENT)) {
        queryFilter +=
            " AND p." + COLUMN_ARCHIVE + " = FALSE AND po." + PartnerOptionDao.COLUMN_OPTION_CODE
                + " = ? AND p." + COLUMN_STATUS_OFFICIAL + "=TRUE";
      }
    } else if (filter.equals(FILTER_ARCHIVED_PARTNERS)) {
      queryFilter +=
          " AND p." + COLUMN_ARCHIVE + " = TRUE AND lower(p." + COLUMN_FULL_NAME + ") LIKE ?";
    }
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT + queryFilter)) {
      if (!filter.equals(FILTER_ALL_PARTNERS)) {
        if (filter.equals(FILTER_COUNTRY)) {
          stmt.setString(1, value);
          stmt.setString(2, option);
        } else {
          stmt.setString(1, "%" + value.toLowerCase() + "%");
        }
      } else {
        if (userRole.equals(UserDto.ROLE_STUDENT)) {
          stmt.setString(1, option);
        }
      }
      System.out.println(stmt);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          partners.add(populatePartnerDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return partners;
  }

  @Override
  public PartnerDto update(PartnerDto partner) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_UPDATE)) {
      populatePreparedStatement(stmt, partner, SQL_UPDATE);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          partner.setVersion(rs.getInt(1));
        } else {
          throw new ConcurrentModificationException();
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return partner;
  }

  private void populatePreparedStatement(PreparedStatement ps, PartnerDto partner, String query)
      throws SQLException {
    ps.setString(1, partner.getLegalName());
    ps.setString(2, partner.getBusinessName());
    ps.setString(3, partner.getFullName());
    ps.setString(4, partner.getOrganisationType());
    ps.setInt(5, partner.getEmployeeCount());
    ps.setInt(6, partner.getAddress().getId());
    ps.setString(7, partner.getEmail());
    ps.setString(8, partner.getWebsite());
    ps.setString(9, partner.getPhoneNumber());
    ps.setBoolean(10, partner.isOfficial());
    if (query.equals(SQL_UPDATE)) {
      ps.setBoolean(11, partner.isArchived());
      ps.setInt(12, partner.getId());
      ps.setInt(13, partner.getVersion());
    }
  }

  /**
   * Populate a PartnerDto based on a resultSet.
   * 
   * @param rs a cursor pointing to its current row of data
   * @return a partnerDto
   */
  private PartnerDto populatePartnerDto(ResultSet rs) throws SQLException {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(rs.getInt(1));
    partner.setLegalName(rs.getString(2));
    partner.setBusinessName(rs.getString(3));
    partner.setFullName(rs.getString(4));
    partner.setOrganisationType(rs.getString(5));
    partner.setEmployeeCount(rs.getInt(6));
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(rs.getInt(7));
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(16));
    country.setName(rs.getString(17));
    address.setCountry(country);
    partner.setAddress(address);
    partner.setEmail(rs.getString(8));
    partner.setWebsite(rs.getString(9));
    partner.setPhoneNumber(rs.getString(10));
    partner.setStatus(rs.getBoolean(11));
    partner.setArchived(rs.getBoolean(12));
    partner.setVersion(rs.getInt(13));
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(rs.getInt(14));
    programme.setProgrammeName(rs.getString(15));
    partner.setProgramme(programme);

    return partner;
  }

}
