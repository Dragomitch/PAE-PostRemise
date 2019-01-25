package com.dragomitch.ipl.pae.persistence.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

class MobilityChoiceDaoImpl implements MobilityChoiceDao {

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_SELECT = "SELECT mc." + COLUMN_ID + ", mc." + COLUMN_USER_ID
      + ", u." + UserDao.COLUMN_LAST_NAME + ", u." + UserDao.COLUMN_FIRST_NAME + ", op."
      + OptionDao.COLUMN_CODE + ", op." + OptionDao.COLUMN_NAME + ", mc." + COLUMN_PREFERENCE_ORDER
      + ", mc." + COLUMN_MOBILITY_TYPE + ", " + "mc." + COLUMN_ACADEMIC_YEAR + ", mc." + COLUMN_TERM
      + ", mc." + COLUMN_PROGRAMME + ", p." + ProgrammeDao.COLUMN_NAME + ", mc." + COLUMN_COUNTRY
      + ", mc." + COLUMN_SUBMISSION_DATE + ", mc." + COLUMN_PROF_DENIAL_REASON + ", mc."
      + COLUMN_STUDENT_CANCELLATION_REASON + ", mc." + COLUMN_PARTNER + ", pa."
      + PartnerDao.COLUMN_FULL_NAME + ", mc.version" + " FROM " + SCHEMA_NAME + "."
      + UserDao.TABLE_NAME + " u, " + SCHEMA_NAME + "." + OptionDao.TABLE_NAME + " op, "
      + SCHEMA_NAME + "." + ProgrammeDao.TABLE_NAME + " p, " + SCHEMA_NAME + "." + TABLE_NAME
      + " mc " + " LEFT OUTER JOIN " + SCHEMA_NAME + "." + CountryDao.TABLE_NAME + " c ON mc."
      + COLUMN_COUNTRY + " = c." + CountryDao.COLUMN_CODE + " LEFT OUTER JOIN " + SCHEMA_NAME + "."
      + DenialReasonDao.TABLE_NAME + " dr ON " + "dr." + DenialReasonDao.COLUMN_ID + "= mc."
      + COLUMN_PROF_DENIAL_REASON + " LEFT OUTER JOIN " + SCHEMA_NAME + "." + PartnerDao.TABLE_NAME
      + " pa ON pa." + PartnerDao.COLUMN_ID + " = mc." + COLUMN_PARTNER + " LEFT OUTER JOIN "
      + SCHEMA_NAME + "." + MobilityDao.TABLE_NAME + " m ON m." + MobilityDao.COLUMN_ID + " = mc."
      + COLUMN_ID + " WHERE u." + UserDao.COLUMN_ID + " = mc." + COLUMN_USER_ID + " AND u."
      + UserDao.COLUMN_OPTION + " = op. " + OptionDao.COLUMN_CODE + " AND p."
      + ProgrammeDao.COLUMN_ID + "= mc." + COLUMN_PROGRAMME;

  private static final String SQL_CREATE = "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + " ( "
      + COLUMN_USER_ID + ", " + COLUMN_PREFERENCE_ORDER + ", " + COLUMN_MOBILITY_TYPE + ", "
      + COLUMN_ACADEMIC_YEAR + ", " + COLUMN_TERM + ", " + COLUMN_PROGRAMME + ", " + COLUMN_COUNTRY
      + ", " + COLUMN_SUBMISSION_DATE + ", " + COLUMN_PROF_DENIAL_REASON + " ,"
      + COLUMN_STUDENT_CANCELLATION_REASON + ", " + COLUMN_PARTNER + ", version )"
      + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, 1) RETURNING " + COLUMN_ID + ", "
      + COLUMN_SUBMISSION_DATE;

  private static final String SQL_UPDATE =
      "UPDATE " + SCHEMA_NAME + "." + TABLE_NAME + " mc SET ( " + COLUMN_PREFERENCE_ORDER + ", "
          + COLUMN_MOBILITY_TYPE + ", " + COLUMN_ACADEMIC_YEAR + ", " + COLUMN_TERM + ", "
          + COLUMN_PROGRAMME + ", " + COLUMN_COUNTRY + ", " + COLUMN_SUBMISSION_DATE + ", "
          + COLUMN_PROF_DENIAL_REASON + ", " + COLUMN_STUDENT_CANCELLATION_REASON + ", "
          + COLUMN_PARTNER + ", version) = (?,?,?,?,?,?,?,?,?,?, version+1)" + "WHERE mc."
          + COLUMN_ID + " = ? AND mc.version = ? RETURNING version";

  /**
   * Sole constructor for explicit invocations.
   * 
   * @param entityFactory an on-demand object dispenser.
   * @param dalBackendServices backend services.
   */
  @Inject
  public MobilityChoiceDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public MobilityChoiceDto create(MobilityChoiceDto mobilityChoice) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_CREATE)) {
      stmt.setInt(1, mobilityChoice.getUser().getId());
      stmt.setInt(2, mobilityChoice.getPreferenceOrder());
      stmt.setString(3, mobilityChoice.getMobilityType());// CHAR(3)
      stmt.setInt(4, mobilityChoice.getAcademicYear());
      stmt.setInt(5, mobilityChoice.getTerm());
      stmt.setInt(6, mobilityChoice.getProgramme().getId());
      if (mobilityChoice.getCountry() == null) {
        stmt.setNull(7, java.sql.Types.CHAR);
      } else {
        stmt.setString(7, mobilityChoice.getCountry().getCountryCode());// CHAR(2)
      }
      if (mobilityChoice.getDenialReason() == null) {
        stmt.setNull(8, java.sql.Types.INTEGER);
      } else {
        stmt.setInt(8, mobilityChoice.getDenialReason().getId());
      }
      stmt.setString(9, mobilityChoice.getCancellationReason());
      if (mobilityChoice.getPartner() != null) {
        stmt.setInt(10, mobilityChoice.getPartner().getId());
      } else {
        stmt.setNull(10, java.sql.Types.INTEGER);
      }
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          mobilityChoice.setId(rs.getInt(1));
          mobilityChoice.setSubmissionDate(rs.getTimestamp(2).toLocalDateTime());
        }
      }
      mobilityChoice.setVersion(1);
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilityChoice;
  }

  @Override
  public MobilityChoiceDto findById(int mobilityChoiceId) {
    MobilityChoiceDto mobilityChoice = null;
    try (PreparedStatement stmt = dalBackendServices
        .prepareStatement(SQL_SELECT + " AND mc." + MobilityChoiceDao.COLUMN_ID + "= ?")) {
      stmt.setInt(1, mobilityChoiceId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          mobilityChoice = populateMobilityChoice(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilityChoice;
  }

  @Override
  public List<MobilityChoiceDto> findAll(String filter) {
    List<MobilityChoiceDto> mobilitiesChoices = new ArrayList<MobilityChoiceDto>();
    String queryFilter = "";
    boolean yearParameter = false;
    if (filter.equals(FILTER_CANCELED_MOBILITIES_CHOICES)) {
      queryFilter += " AND mc." + COLUMN_STUDENT_CANCELLATION_REASON + " IS NOT NULL";
    } else if (filter.equals(FILTER_REJECTED_MOBILITIES_CHOICES)) {
      queryFilter += " AND mc." + COLUMN_PROF_DENIAL_REASON + " IS NOT NULL";
    } else if (filter.equals(FILTER_PASSED_MOBILITIES_CHOICES)) {
      queryFilter += " AND mc." + COLUMN_ACADEMIC_YEAR + " < ?";
      yearParameter = true;
    } else if (filter.equals(FILTER_ACTIVE_MOBILITIES_CHOICES)) {
      queryFilter += " AND mc." + COLUMN_ACADEMIC_YEAR + " = ? AND mc." + COLUMN_PROF_DENIAL_REASON
          + " IS NULL AND mc." + COLUMN_STUDENT_CANCELLATION_REASON + " IS NULL AND" + " mc."
          + COLUMN_ID + " NOT IN (SELECT m." + MobilityDao.COLUMN_ID + " FROM " + SCHEMA_NAME + "."
          + MobilityDao.TABLE_NAME + " m )";
      yearParameter = true;
    }
    try (PreparedStatement findAllStatement =
        dalBackendServices.prepareStatement(SQL_SELECT + queryFilter)) {
      if (yearParameter) {
        findAllStatement.setInt(1, LocalDate.now().getYear());
      }
      try (ResultSet rs = findAllStatement.executeQuery()) {
        while (rs.next()) {
          mobilitiesChoices.add(populateMobilityChoice(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilitiesChoices;
  }

  @Override
  public void update(MobilityChoiceDto mobilityChoice) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_UPDATE)) {
      stmt.setInt(1, mobilityChoice.getPreferenceOrder());
      stmt.setString(2, mobilityChoice.getMobilityType());
      stmt.setInt(3, mobilityChoice.getAcademicYear());
      stmt.setInt(4, mobilityChoice.getTerm());
      stmt.setInt(5, mobilityChoice.getProgramme().getId());
      stmt.setString(6, mobilityChoice.getCountry().getCountryCode());
      stmt.setTimestamp(7, Timestamp.valueOf(mobilityChoice.getSubmissionDate()));
      if (mobilityChoice.getDenialReason() == null) {
        stmt.setNull(8, java.sql.Types.INTEGER);
      } else {
        stmt.setInt(8, mobilityChoice.getDenialReason().getId());
      }
      if (isAValidString(mobilityChoice.getCancellationReason())) {
        stmt.setString(9, mobilityChoice.getCancellationReason());
      } else {
        stmt.setNull(9, java.sql.Types.VARCHAR);
      }
      if (mobilityChoice.getPartner() == null) {
        stmt.setNull(10, java.sql.Types.INTEGER);
      } else {
        stmt.setInt(10, mobilityChoice.getPartner().getId());
      }
      stmt.setInt(11, mobilityChoice.getId());
      stmt.setInt(12, mobilityChoice.getVersion());
      try (ResultSet rs = stmt.executeQuery()) {
        int res = 0;
        if (rs.next()) {
          res = rs.getInt(1);
        }
        if (res == 0) {
          throw new ConcurrentModificationException(
              "The data have been modified before that query");
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }

  /**
   * Queries the database to find mobilities choices where the columnName is columnValue.
   * 
   * @param columnName the column name we are going to look into
   * @param columnValue the value of the columnName we are looking for
   * @return a list of results were the columnName have columnValue as value
   */
  private List<MobilityChoiceDto> findBy(String columnName, Object columnValue) {
    List<MobilityChoiceDto> mobilitiesChoices = new ArrayList<MobilityChoiceDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT + " AND mc."
        + columnName + " = ?" + " AND mc." + COLUMN_ID + " NOT IN (SELECT m."
        + MobilityDao.COLUMN_ID + " FROM " + SCHEMA_NAME + "." + MobilityDao.TABLE_NAME + " m )")) {
      stmt.setObject(1, columnValue.getClass().cast(columnValue));
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          mobilitiesChoices.add(populateMobilityChoice(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilitiesChoices;
  }

  @Override
  public List<MobilityChoiceDto> findByUser(int userId) {
    return findBy(COLUMN_USER_ID, userId);
  }

  @Override
  public List<MobilityChoiceDto> findByPartner(int partnerId) {
    return findBy(COLUMN_PARTNER, partnerId);
  }

  @Override
  public List<MobilityChoiceDto> findByActivePartner(int partnerId) {
    List<MobilityChoiceDto> mobilitiesChoices = new ArrayList<MobilityChoiceDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT + " AND mc."
        + COLUMN_PARTNER + " = ?" + " AND mc." + COLUMN_ACADEMIC_YEAR + " = ? AND mc."
        + COLUMN_PROF_DENIAL_REASON + " IS NULL AND mc." + COLUMN_STUDENT_CANCELLATION_REASON
        + " IS NULL AND mc." + COLUMN_ID + " NOT IN ( SELECT m." + MobilityDao.COLUMN_ID + " FROM "
        + SCHEMA_NAME + "." + MobilityDao.TABLE_NAME + " m WHERE m."
        + MobilityDao.COLUMN_PROF_DENIAL_REASON + " IS NOT NULL AND m."
        + MobilityDao.COLUMN_STUDENT_CANCELLATION_REASON + " IS NOT NULL)")) {
      stmt.setInt(1, partnerId);
      stmt.setInt(2, LocalDate.now().getYear());
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          mobilitiesChoices.add(populateMobilityChoice(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilitiesChoices;
  }

  /**
   * construct a MobilityChoiceDto from a ResultSet which contain a valid row for a MobilityChoice.
   * The ResultSet need to contain a full row from the DB, as in the 'FIND_BY' Query.
   * 
   * @param rs the resulSet retrieved by the DataBase.
   * @return the MobilityChoiceDto corresponding the row given.
   */
  private MobilityChoiceDto populateMobilityChoice(ResultSet rs) throws SQLException {
    MobilityChoiceDto mobilityChoice =
        (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    UserDto user = (UserDto) entityFactory.build(UserDto.class);

    mobilityChoice.setId(rs.getInt(1));
    user.setId(rs.getInt(2));
    user.setLastName(rs.getString(3));
    user.setFirstName(rs.getString(4));
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(rs.getString(5));
    option.setName(rs.getString(6));
    user.setOption(option);
    mobilityChoice.setUser(user);
    mobilityChoice.setPreferenceOrder(rs.getInt(7));
    mobilityChoice.setMobilityType(rs.getString(8));
    mobilityChoice.setAcademicYear(rs.getInt(9));
    mobilityChoice.setTerm(rs.getInt(10));
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(rs.getInt(11));
    programme.setProgrammeName(rs.getString(12));
    mobilityChoice.setProgramme(programme);
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(13));
    mobilityChoice.setCountry(country);
    mobilityChoice.setSubmissionDate(rs.getTimestamp(14).toLocalDateTime());
    if (rs.getInt(15) > 0) {
      DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
      denialReason.setId(rs.getInt(15));
      mobilityChoice.setDenialReason(denialReason);
    } else {
      mobilityChoice.setDenialReason(null);
    }
    mobilityChoice.setCancellationReason(rs.getString(16));
    if (rs.getInt(17) > 0) {
      PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
      partner.setId(rs.getInt(17));
      partner.setFullName(rs.getString(18));
      mobilityChoice.setPartner(partner);
    } else {
      mobilityChoice.setPartner(null);
    }
    mobilityChoice.setVersion(rs.getInt(19));
    return mobilityChoice;

  }


}
