package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import org.springframework.stereotype.Repository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
class PartnerDaoImpl implements PartnerDao {

  private static final String SCHEMA = DalBackendServices.SCHEMA_NAME;

  private static final String SQL_INSERT = "INSERT INTO " + SCHEMA + "." + TABLE_NAME + " ("
      + COLUMN_LEGAL_NAME + ", " + COLUMN_BUSINESS_NAME + ", " + COLUMN_FULL_NAME + ", "
      + COLUMN_ORGANISATION_TYPE + ", " + COLUMN_EMPLOYEE_COUNT + ", " + COLUMN_ADDRESS + ", "
      + COLUMN_EMAIL + ", " + COLUMN_WEBSITE + ", " + COLUMN_PHONE_NUMBER + ", "
      + COLUMN_STATUS_OFFICIAL + ", " + COLUMN_ARCHIVE + ", version) "
      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, 1) RETURNING partner_id";

  /**
   * Partners with their address country, programme and options. The options are LEFT-joined so
   * that a partner without any option is still found (with an empty option list); a partner with
   * several options comes back as several rows that {@link #readPartners(ResultSet)} merges.
   */
  private static final String SQL_SELECT = "SELECT p." + COLUMN_ID + ", p." + COLUMN_LEGAL_NAME
      + ", p." + COLUMN_BUSINESS_NAME + ", p." + COLUMN_FULL_NAME + ", p."
      + COLUMN_ORGANISATION_TYPE + ", p." + COLUMN_EMPLOYEE_COUNT + ", p." + COLUMN_ADDRESS + ", p."
      + COLUMN_EMAIL + ", p." + COLUMN_WEBSITE + ", p." + COLUMN_PHONE_NUMBER + ", p."
      + COLUMN_STATUS_OFFICIAL + ", p." + COLUMN_ARCHIVE + ", p." + COLUMN_VERSION
      + ", pr.programme_id, pr.name, c.country_code, c.name, po."
      + PartnerOptionDao.COLUMN_OPTION_CODE + ", po." + PartnerOptionDao.COLUMN_DEPARTEMENT + ", o."
      + OptionDao.COLUMN_NAME + " FROM " + SCHEMA + "." + TABLE_NAME + " p JOIN " + SCHEMA + "."
      + AddressDao.TABLE_NAME + " a ON p." + COLUMN_ADDRESS + " = a." + AddressDao.COLUMN_ID
      + " JOIN " + SCHEMA + "." + CountryDao.TABLE_NAME + " c ON a." + AddressDao.COLUMN_COUNTRY
      + " = c." + CountryDao.COLUMN_CODE + " JOIN " + SCHEMA + "." + ProgrammeDao.TABLE_NAME
      + " pr ON c." + CountryDao.COLUMN_PROGRAMME_ID + " = pr." + ProgrammeDao.COLUMN_ID
      + " LEFT JOIN " + SCHEMA + "." + PartnerOptionDao.TABLE_NAME + " po ON po."
      + PartnerOptionDao.COLUMN_PARTNER_ID + " = p." + COLUMN_ID + " LEFT JOIN " + SCHEMA + "."
      + OptionDao.TABLE_NAME + " o ON o." + OptionDao.COLUMN_CODE + " = po."
      + PartnerOptionDao.COLUMN_OPTION_CODE;

  private static final String SQL_ORDER = " ORDER BY p." + COLUMN_ID + ", po."
      + PartnerOptionDao.COLUMN_OPTION_CODE;

  /** Restricts a query to the partners offering the option bound to the parameter. */
  private static final String HAS_OPTION = "EXISTS (SELECT 1 FROM " + SCHEMA + "."
      + PartnerOptionDao.TABLE_NAME + " f WHERE f." + PartnerOptionDao.COLUMN_PARTNER_ID + " = p."
      + COLUMN_ID + " AND f." + PartnerOptionDao.COLUMN_OPTION_CODE + " = ?)";

  private static final String SQL_UPDATE = "UPDATE " + SCHEMA + "." + TABLE_NAME + " p SET ("
      + COLUMN_LEGAL_NAME + ", " + COLUMN_BUSINESS_NAME + ", " + COLUMN_FULL_NAME + ", "
      + COLUMN_ORGANISATION_TYPE + ", " + COLUMN_EMPLOYEE_COUNT + ", " + COLUMN_ADDRESS + ", "
      + COLUMN_EMAIL + ", " + COLUMN_WEBSITE + ", " + COLUMN_PHONE_NUMBER + ", "
      + COLUMN_STATUS_OFFICIAL + ", " + COLUMN_ARCHIVE + ", " + COLUMN_VERSION
      + ") = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version+1) WHERE p." + COLUMN_ID + " = ? AND p."
      + COLUMN_VERSION + " = ? RETURNING p." + COLUMN_VERSION;


  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

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
    try (PreparedStatement stmt = dalBackendServices
        .prepareStatement(SQL_SELECT + " WHERE p." + COLUMN_ID + " = ?" + SQL_ORDER)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        List<PartnerDto> partners = readPartners(rs);
        return partners.isEmpty() ? null : partners.get(0);
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Professors see every partner matching the filter. Students only see official, non-archived
   * partners offering their option ({@code "all"} and {@code "country"} filters).
   *
   * @throws BusinessException INVALID_PARTNER_FILTER_709 if the filter is not one of the
   *         {@code FILTER_*} constants
   */
  @Override
  public List<PartnerDto> findAll(String filter, String value, String userRole, String option) {
    boolean student = UserDto.ROLE_STUDENT.equals(userRole);
    List<String> conditions = new ArrayList<String>();
    List<String> parameters = new ArrayList<String>();
    if (FILTER_COUNTRY.equals(filter)) {
      conditions.add("c." + CountryDao.COLUMN_CODE + " = ?");
      parameters.add(value);
    } else if (FILTER_ARCHIVED_PARTNERS.equals(filter)) {
      conditions.add("p." + COLUMN_ARCHIVE + " = TRUE");
      conditions.add("lower(p." + COLUMN_FULL_NAME + ") LIKE ?");
      parameters.add("%" + value.toLowerCase() + "%");
    } else if (!FILTER_ALL_PARTNERS.equals(filter)) {
      throw new BusinessException(ErrorFormat.INVALID_PARTNER_FILTER_709);
    }
    if (student && !FILTER_ARCHIVED_PARTNERS.equals(filter)) {
      conditions.add("p." + COLUMN_ARCHIVE + " = FALSE");
      conditions.add("p." + COLUMN_STATUS_OFFICIAL + " = TRUE");
      conditions.add(HAS_OPTION);
      parameters.add(option);
    }
    String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + where + SQL_ORDER)) {
      for (int i = 0; i < parameters.size(); i++) {
        stmt.setString(i + 1, parameters.get(i));
      }
      try (ResultSet rs = stmt.executeQuery()) {
        return readPartners(rs);
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
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
   * Reads the rows of {@link #SQL_SELECT} (ordered by partner): one partner per id, with the
   * options of its rows.
   *
   * @param rs the rows
   * @return the partners, in the order of the rows
   */
  private List<PartnerDto> readPartners(ResultSet rs) throws SQLException {
    Map<Integer, PartnerDto> partners = new LinkedHashMap<Integer, PartnerDto>();
    while (rs.next()) {
      PartnerDto partner = partners.get(rs.getInt(1));
      if (partner == null) {
        partner = populatePartnerDto(rs);
        partner.setOptions(new ArrayList<PartnerOptionDto>());
        partners.put(partner.getId(), partner);
      }
      String optionCode = rs.getString(18);
      if (optionCode != null) {
        PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
        option.setCode(optionCode);
        option.setDepartement(rs.getString(19));
        option.setName(rs.getString(20));
        partner.getOptions().add(option);
      }
    }
    return new ArrayList<PartnerDto>(partners.values());
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
