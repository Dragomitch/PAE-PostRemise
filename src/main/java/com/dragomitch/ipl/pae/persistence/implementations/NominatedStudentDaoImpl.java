package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

class NominatedStudentDaoImpl implements NominatedStudentDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_INSERT = "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + "("
      + COLUMN_ID + ", " + COLUMN_TITLE + ", " + COLUMN_BIRTHDATE + ", " + COLUMN_NATIONALITY + ", "
      + COLUMN_PHONE_NUMBER + ", " + COLUMN_GENDER + ", " + COLUMN_PASSED_YEARS_COUNT + ", "
      + COLUMN_IBAN + ", " + COLUMN_CARD_HOLDER + ", " + COLUMN_BANK_NAME + ", " + COLUMN_BIC + ", "
      + COLUMN_ADDRESS + ", version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_UPDATE = "UPDATE " + SCHEMA_NAME + "." + TABLE_NAME + " SET ("
      + COLUMN_TITLE + ", " + COLUMN_BIRTHDATE + ", " + COLUMN_NATIONALITY + ", "
      + COLUMN_PHONE_NUMBER + ", " + COLUMN_GENDER + ", " + COLUMN_PASSED_YEARS_COUNT + ", "
      + COLUMN_IBAN + ", " + COLUMN_CARD_HOLDER + ", " + COLUMN_BANK_NAME + ", " + COLUMN_BIC
      + ", version) = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version + 1) " + "WHERE " + COLUMN_ID
      + " = ? AND version = ? RETURNING version";

  private static final String SQL_FIND = "SELECT u." + UserDao.COLUMN_ID + ", u."
      + UserDao.COLUMN_LAST_NAME + ", u." + UserDao.COLUMN_FIRST_NAME + ", u."
      + UserDao.COLUMN_USERNAME + ", u." + UserDao.COLUMN_PASSWORD + ", u." + UserDao.COLUMN_EMAIL
      + ", u." + UserDao.COLUMN_REGISTRATION_DATE + ",u." + UserDao.COLUMN_ROLE + ", o."
      + OptionDao.COLUMN_CODE + ", o." + OptionDao.COLUMN_NAME + ", ns." + COLUMN_TITLE + ", ns."
      + COLUMN_BIRTHDATE + ", ns." + COLUMN_NATIONALITY + ", c." + CountryDao.COLUMN_NAME + ", ns."
      + COLUMN_PHONE_NUMBER + ", ns." + COLUMN_GENDER + ", ns." + COLUMN_PASSED_YEARS_COUNT
      + ", ns." + COLUMN_IBAN + ", ns." + COLUMN_CARD_HOLDER + ", ns." + COLUMN_BANK_NAME + ", ns."
      + COLUMN_BIC + ", ns." + COLUMN_ADDRESS + ", ns.version FROM " + SCHEMA_NAME + "."
      + UserDao.TABLE_NAME + " u, " + SCHEMA_NAME + "." + TABLE_NAME + " ns, " + SCHEMA_NAME + "."
      + OptionDao.TABLE_NAME + " o, " + SCHEMA_NAME + "." + CountryDao.TABLE_NAME + " c WHERE u."
      + UserDao.COLUMN_OPTION + " = o." + OptionDao.COLUMN_CODE + " AND u." + UserDao.COLUMN_ID
      + " = ns." + COLUMN_ID + " AND ns." + COLUMN_NATIONALITY + " = c." + CountryDao.COLUMN_CODE;

  private final EntityFactory entityFactory;
  private final DalBackendServices dalServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public NominatedStudentDaoImpl(EntityFactory entityFactory, DalBackendServices dalServices) {
    this.entityFactory = entityFactory;
    this.dalServices = dalServices;
  }

  @Override
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent) {
    try (PreparedStatement stmt = dalServices.prepareStatement(SQL_INSERT)) {
      stmt.setInt(1, nominatedStudent.getId());
      populatePreparedStatement(stmt, nominatedStudent, 2);
      stmt.setInt(12, nominatedStudent.getAddress().getId());
      stmt.setInt(13, nominatedStudent.getVersion());
      stmt.execute();
      nominatedStudent.setVersion(1);
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return nominatedStudent;
  }

  @Override
  public NominatedStudentDto findById(int id) {
    NominatedStudentDto nominatedStudent = null;
    try (PreparedStatement stmt =
        dalServices.prepareStatement(SQL_FIND + " AND ? = ns." + COLUMN_ID)) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          nominatedStudent = populateStudentDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return nominatedStudent;
  }

  @Override
  public List<NominatedStudentDto> findAll() {
    List<NominatedStudentDto> nominatedStudents = new ArrayList<NominatedStudentDto>();
    try (PreparedStatement stmt = dalServices.prepareStatement(SQL_FIND)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          nominatedStudents.add(populateStudentDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return nominatedStudents;
  }

  @Override
  public NominatedStudentDto update(NominatedStudentDto nominatedStudent) {
    try (PreparedStatement stmt = dalServices.prepareStatement(SQL_UPDATE)) {
      populatePreparedStatement(stmt, nominatedStudent, 1);
      stmt.setInt(11, nominatedStudent.getId());
      stmt.setInt(12, nominatedStudent.getVersion());
      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new ConcurrentModificationException();
        }
        nominatedStudent.setVersion(rs.getInt(1));
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return nominatedStudent;
  }

  private void populatePreparedStatement(PreparedStatement ps, NominatedStudentDto nominatedStudent,
      int parameterIndex) throws SQLException {
    ps.setString(parameterIndex++, nominatedStudent.getTitle());
    ps.setTimestamp(parameterIndex++,
        Timestamp.valueOf(nominatedStudent.getBirthdate().atStartOfDay()));
    ps.setString(parameterIndex++, nominatedStudent.getNationality().getCountryCode());
    ps.setString(parameterIndex++, nominatedStudent.getPhoneNumber());
    ps.setString(parameterIndex++, nominatedStudent.getGender());
    ps.setInt(parameterIndex++, nominatedStudent.getNbrPassedYears());
    ps.setString(parameterIndex++, nominatedStudent.getIban());
    if (nominatedStudent.getCardHolder() == null) {
      ps.setNull(parameterIndex++, java.sql.Types.VARCHAR);
    } else {
      ps.setString(parameterIndex++, nominatedStudent.getCardHolder());
    }
    ps.setString(parameterIndex++, nominatedStudent.getBankName());
    ps.setString(parameterIndex++, nominatedStudent.getBic());
  }

  private NominatedStudentDto populateStudentDto(ResultSet rs) throws SQLException {
    NominatedStudentDto nominatedStudent =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    nominatedStudent.setId(rs.getInt(1));
    nominatedStudent.setLastName(rs.getString(2));
    nominatedStudent.setFirstName(rs.getString(3));
    nominatedStudent.setUsername(rs.getString(4));
    nominatedStudent.setPassword(rs.getString(5));
    nominatedStudent.setEmail(rs.getString(6));
    nominatedStudent.setRegistrationDate(rs.getTimestamp(7).toLocalDateTime());
    nominatedStudent.setRole(rs.getString(8));
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(rs.getString(9));
    option.setName(rs.getString(10));
    nominatedStudent.setOption(option);
    nominatedStudent.setTitle(rs.getString(11));
    nominatedStudent.setBirthdate(rs.getTimestamp(12).toLocalDateTime().toLocalDate());
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(13));
    country.setName(rs.getString(14));
    nominatedStudent.setNationality(country);
    nominatedStudent.setPhoneNumber(rs.getString(15));
    nominatedStudent.setGender(rs.getString(16));
    nominatedStudent.setNbrPassedYears(rs.getInt(17));
    nominatedStudent.setIban(rs.getString(18));
    String cardHolder = rs.getString(19);
    if (rs.wasNull()) {
      cardHolder = null;
    }
    nominatedStudent.setCardHolder(cardHolder);
    nominatedStudent.setBankName(rs.getString(20));
    nominatedStudent.setBic(rs.getString(21));
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(rs.getInt(22));
    nominatedStudent.setAddress(address);
    nominatedStudent.setVersion(rs.getInt(23));
    return nominatedStudent;
  }
}
