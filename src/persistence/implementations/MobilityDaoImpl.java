package persistence.implementations;

import business.EntityFactory;
import business.dto.CountryDto;
import business.dto.DenialReasonDto;
import business.dto.MobilityDto;
import business.dto.NominatedStudentDto;
import business.dto.OptionDto;
import business.dto.PartnerDto;
import business.dto.ProgrammeDto;
import business.dto.UserDto;
import main.ContextManager;
import main.annotations.Inject;
import main.exceptions.FatalException;
import persistence.CountryDao;
import persistence.DenialReasonDao;
import persistence.MobilityChoiceDao;
import persistence.MobilityDao;
import persistence.PartnerDao;
import persistence.ProgrammeDao;
import persistence.UserDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class MobilityDaoImpl implements MobilityDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String INSERT =
      "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + "(" + COLUMN_ID + ", "
          + COLUMN_SUBMISSION_DATE + ", " + COLUMN_STATE + ", " + COLUMN_STATE_BEFORE_CANCELLATION
          + ", " + COLUMN_FIRST_PAYMENT_REQUEST_DATE + ", " + COLUMN_SECOND_PAYMENT_REQUEST_DATE
          + ", " + COLUMN_PRO_ECO_ENCODING + ", " + COLUMN_SECOND_SOFTWARE_ENCODING + ", "
          + COLUMN_STUDENT_CANCELLATION_REASON + ", " + COLUMN_PROF_DENIAL_REASON + ", "
          + COLUMN_PROFESSOR_IN_CHARGE + ", version) VALUES (?,?,?,?,?,?,?,?,?,?,?, 1)";

  private static final String SELECT = "SELECT mc." + MobilityChoiceDao.COLUMN_ID + ", mc."
      + MobilityChoiceDao.COLUMN_MOBILITY_TYPE + ", mc." + MobilityChoiceDao.COLUMN_ACADEMIC_YEAR
      + ", mc." + MobilityChoiceDao.COLUMN_TERM + ", m." + COLUMN_SUBMISSION_DATE + ", m."
      + COLUMN_STATE + ", m." + COLUMN_STATE_BEFORE_CANCELLATION + ", m."
      + COLUMN_FIRST_PAYMENT_REQUEST_DATE + ", m." + COLUMN_SECOND_PAYMENT_REQUEST_DATE + ", m."
      + COLUMN_PRO_ECO_ENCODING + ", m." + COLUMN_SECOND_SOFTWARE_ENCODING + ", m."
      + COLUMN_STUDENT_CANCELLATION_REASON + ", m." + COLUMN_PROF_DENIAL_REASON + ", dr."
      + DenialReasonDao.COLUMN_REASON + ", m." + COLUMN_PROFESSOR_IN_CHARGE + ", m.version, mc."
      + MobilityChoiceDao.COLUMN_USER_ID + ", u." + UserDao.COLUMN_FIRST_NAME + ", u."
      + UserDao.COLUMN_LAST_NAME + ", u. " + UserDao.COLUMN_OPTION + ", pa." + PartnerDao.COLUMN_ID
      + ", pa." + PartnerDao.COLUMN_FULL_NAME + ", c." + CountryDao.COLUMN_CODE + ", c."
      + CountryDao.COLUMN_NAME + ", p." + ProgrammeDao.COLUMN_ID + ", p." + ProgrammeDao.COLUMN_NAME
      + " FROM " + SCHEMA_NAME + "." + MobilityChoiceDao.TABLE_NAME + " mc JOIN " + SCHEMA_NAME
      + "." + TABLE_NAME + " m ON mc." + COLUMN_ID + " = m." + COLUMN_ID + " JOIN " + SCHEMA_NAME
      + "." + UserDao.TABLE_NAME + " u ON mc." + MobilityChoiceDao.COLUMN_USER_ID + " = u."
      + UserDao.COLUMN_ID + " JOIN " + SCHEMA_NAME + "." + PartnerDao.TABLE_NAME + " pa ON mc."
      + MobilityChoiceDao.COLUMN_PARTNER + " = pa." + PartnerDao.COLUMN_ID + " JOIN " + SCHEMA_NAME
      + "." + CountryDao.TABLE_NAME + " c ON " + MobilityChoiceDao.COLUMN_COUNTRY + " = c."
      + CountryDao.COLUMN_CODE + " JOIN " + SCHEMA_NAME + "." + ProgrammeDao.TABLE_NAME
      + " p ON mc." + MobilityChoiceDao.COLUMN_PROGRAMME + " = p." + ProgrammeDao.COLUMN_ID
      + " LEFT OUTER JOIN " + SCHEMA_NAME + "." + DenialReasonDao.TABLE_NAME + " dr ON m."
      + COLUMN_PROF_DENIAL_REASON + " = dr." + DenialReasonDao.COLUMN_ID;

  private static final String UPDATE = "UPDATE " + SCHEMA_NAME + "." + TABLE_NAME + " SET ("
      + COLUMN_SUBMISSION_DATE + ", " + COLUMN_STATE + ", " + COLUMN_STATE_BEFORE_CANCELLATION
      + ", " + COLUMN_FIRST_PAYMENT_REQUEST_DATE + ", " + COLUMN_SECOND_PAYMENT_REQUEST_DATE + ", "
      + COLUMN_PRO_ECO_ENCODING + ", " + COLUMN_SECOND_SOFTWARE_ENCODING + ", "
      + COLUMN_STUDENT_CANCELLATION_REASON + ", " + COLUMN_PROF_DENIAL_REASON + ", "
      + COLUMN_PROFESSOR_IN_CHARGE + ", version) = (?,?,?,?,?,?,?,?,?,?, version + 1) " + "WHERE "
      + COLUMN_ID + " = ? AND version = ? RETURNING version";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalServices backend services
   */
  @Inject
  public MobilityDaoImpl(EntityFactory entityFactory, DalBackendServices dalServices) {
    this.entityFactory = entityFactory;
    this.dalServices = dalServices;
  }

  @Override
  public MobilityDto create(MobilityDto mobility) {
    try (PreparedStatement stmt = dalServices.prepareStatement(INSERT)) {
      stmt.setInt(1, mobility.getId());
      populatePreparedStatement(stmt, mobility, 2);
      stmt.execute();
      mobility.setVersion(1);
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobility;
  }

  @Override
  public MobilityDto findById(int id) {
    MobilityDto mobility = null;
    try (PreparedStatement stmt =
        dalServices.prepareStatement(SELECT + " WHERE m." + COLUMN_ID + " = ?")) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          mobility = populateMobilityDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobility;
  }

  @Override
  public List<MobilityDto> findAll() {
    List<MobilityDto> mobilities = new ArrayList<MobilityDto>();
    try (PreparedStatement stmt = dalServices.prepareStatement(SELECT)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          mobilities.add(populateMobilityDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilities;
  }

  @Override
  public List<MobilityDto> findByUser(int user) {
    return findBy("u." + MobilityChoiceDao.COLUMN_USER_ID, user);
  }

  private List<MobilityDto> findBy(String column, Object value) {
    String sql = SELECT + " WHERE " + column + " = ?";
    List<MobilityDto> mobilities = new ArrayList<MobilityDto>();
    try (PreparedStatement stmt = dalServices.prepareStatement(sql)) {
      stmt.setObject(1, value);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          mobilities.add(populateMobilityDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobilities;
  }

  @Override
  public MobilityDto update(MobilityDto mobility) {
    try (PreparedStatement stmt = dalServices.prepareStatement(UPDATE)) {
      populatePreparedStatement(stmt, mobility, 1);
      stmt.setInt(11, mobility.getId());
      stmt.setInt(12, mobility.getVersion());
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new ConcurrentModificationException();
        }
        mobility.setVersion(rs.getInt(1));
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return mobility;
  }

  /**
   * Populates a PreparedStatement instance.
   * 
   * @param ps the PreparedStatement instance to be populated
   * @param mobility the mobility from which to take the data
   * @param parameterIndex the beginning parameter
   * @throws SQLException if an SQL error occurs
   */
  private void populatePreparedStatement(PreparedStatement ps, MobilityDto mobility,
      int parameterIndex) throws SQLException {
    ps.setTimestamp(parameterIndex++, Timestamp.valueOf(mobility.getSubmissionDate()));
    ps.setString(parameterIndex++, mobility.getState());
    ps.setString(parameterIndex++, mobility.getStateBeforeCancellation());
    if (mobility.getFirstPaymentRequestDate() == null) {
      ps.setNull(parameterIndex++, Types.TIMESTAMP);
    } else {
      ps.setTimestamp(parameterIndex++, Timestamp.valueOf(mobility.getFirstPaymentRequestDate()));
    }
    if (mobility.getSecondPaymentRequestDate() == null) {
      ps.setNull(parameterIndex++, Types.TIMESTAMP);
    } else {
      ps.setTimestamp(parameterIndex++, Timestamp.valueOf(mobility.getSecondPaymentRequestDate()));
    }
    ps.setBoolean(parameterIndex++, mobility.isEncodedInProEco());
    ps.setBoolean(parameterIndex++, mobility.isEncodedInSecondSoftware());
    ps.setString(parameterIndex++, mobility.getCancellationReason());
    if (mobility.getDenialReason() == null) {
      ps.setNull(parameterIndex++, Types.INTEGER);
    } else {
      ps.setInt(parameterIndex++, mobility.getDenialReason().getId());
    }
    if (mobility.getProfessorInCharge() == null) {
      ps.setNull(parameterIndex++, Types.INTEGER);
    } else {
      ps.setInt(parameterIndex++, mobility.getProfessorInCharge().getId());
    }
  }

  private MobilityDto populateMobilityDto(ResultSet rs) throws SQLException {
    MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
    mobility.setId(rs.getInt(1));
    mobility.setMobilityType(rs.getString(2));
    mobility.setAcademicYear(rs.getInt(3)); // TODO Gestion années académiques
    mobility.setTerm(rs.getInt(4));
    mobility.setSubmissionDate(rs.getTimestamp(5).toLocalDateTime());
    mobility.setState(rs.getString(6));
    mobility.setStateBeforeCancellation(rs.getString(7));
    if (rs.getTimestamp(8) != null) {
      mobility.setFirstPaymentRequestDate(rs.getTimestamp(8).toLocalDateTime());
    }
    if (rs.getTimestamp(9) != null) {
      mobility.setSecondPaymentRequestDate(rs.getTimestamp(9).toLocalDateTime());
    }
    mobility.setProEcoEncoding(rs.getBoolean(10));
    mobility.setSecondSoftwareEncoding(rs.getBoolean(11));
    mobility.setCancellationReason(rs.getString(12));
    int denialReasonId = rs.getInt(13);
    if (rs.wasNull()) {
      mobility.setDenialReason(null);
    } else {
      DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
      denialReason.setId(denialReasonId);
      denialReason.setReason(rs.getString(14));
      mobility.setDenialReason(denialReason);
    }
    int professorInChargeId = rs.getInt(15);
    if (!rs.wasNull()) {
      UserDto professor = (UserDto) entityFactory.build(UserDto.class);
      professor.setId(professorInChargeId);
      mobility.setProfessorInCharge(professor);
    }
    mobility.setVersion(rs.getInt(16));
    NominatedStudentDto nominatedStudent =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    nominatedStudent.setId(rs.getInt(17));
    nominatedStudent.setFirstName(rs.getString(18));
    nominatedStudent.setLastName(rs.getString(19));
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(rs.getString(20));
    nominatedStudent.setOption(option);
    mobility.setNominatedStudent(nominatedStudent);
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(rs.getInt(21));
    partner.setFullName(rs.getString(22));
    mobility.setPartner(partner);
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(23));
    country.setName(rs.getString(24));
    mobility.setCountry(country);
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(rs.getInt(25));
    programme.setProgrammeName(rs.getString(26));
    mobility.setProgramme(programme);
    return mobility;
  }

}
