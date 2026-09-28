package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link NominatedStudentDao} with Spring's {@link JdbcClient}. A nominated student extends a
 * user (same id): the rows of both tables are read together, joined with the option and the
 * nationality. Updates check the version.
 */
@Repository
class NominatedStudentDaoImpl implements NominatedStudentDao {

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.nominated_students
        (user_id, title, birthdate, nationality, phone_number, gender, passed_years_count, iban,
         card_holder, bank_name, bic, address, version)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

  private static final String SQL_UPDATE = """
      UPDATE student_exchange_tools.nominated_students
         SET (title, birthdate, nationality, phone_number, gender, passed_years_count, iban,
              card_holder, bank_name, bic, version)
           = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version + 1)
       WHERE user_id = ? AND version = ?
      RETURNING version""";

  private static final String SQL_SELECT = """
      SELECT u.user_id, u.last_name, u.first_name, u.username, u.password, u.email,
             u.registration_date, u.role, o.option_code, o.name, ns.title, ns.birthdate,
             ns.nationality, c.name, ns.phone_number, ns.gender, ns.passed_years_count, ns.iban,
             ns.card_holder, ns.bank_name, ns.bic, ns.address, ns.version
        FROM student_exchange_tools.users u
        JOIN student_exchange_tools.nominated_students ns ON u.user_id = ns.user_id
        JOIN student_exchange_tools.options o ON u.option = o.option_code
        JOIN student_exchange_tools.countries c ON ns.nationality = c.country_code""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  NominatedStudentDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The row is inserted with the version of the DTO (the user's), which the DTO keeps.
   */
  @Override
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent) {
    DataAccess.run(() -> bindColumns(jdbcClient.sql(SQL_INSERT).param(nominatedStudent.getId()),
        nominatedStudent).param(nominatedStudent.getAddress().getId())
        .param(nominatedStudent.getVersion()).update());
    return nominatedStudent;
  }

  @Override
  public NominatedStudentDto findById(int id) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " WHERE ns.user_id = ?").param(id)
        .query(this::toDto).list().stream().findFirst().orElse(null));
  }

  @Override
  public List<NominatedStudentDto> findAll() {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT).query(this::toDto).list());
  }

  @Override
  public NominatedStudentDto update(NominatedStudentDto nominatedStudent) {
    int version = DataAccess.call(() -> bindColumns(jdbcClient.sql(SQL_UPDATE), nominatedStudent)
        .param(nominatedStudent.getId()).param(nominatedStudent.getVersion())
        .query(Integer.class).optional().orElseThrow(ConcurrentModificationException::new));
    nominatedStudent.setVersion(version);
    return nominatedStudent;
  }

  /** Binds the updatable columns, title to bic, in the order of the statements above. */
  private static JdbcClient.StatementSpec bindColumns(JdbcClient.StatementSpec statement,
      NominatedStudentDto student) {
    return statement.param(student.getTitle())
        .param(Timestamp.valueOf(student.getBirthdate().atStartOfDay()))
        .param(student.getNationality().getCountryCode())
        .param(student.getPhoneNumber())
        .param(student.getGender())
        .param(student.getNbrPassedYears())
        .param(student.getIban())
        .param(DataAccess.typed(Types.VARCHAR, student.getCardHolder()))
        .param(student.getBankName())
        .param(student.getBic());
  }

  private NominatedStudentDto toDto(ResultSet rs, int rowNum) throws SQLException {
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
    nominatedStudent.setCardHolder(rs.getString(19));
    nominatedStudent.setBankName(rs.getString(20));
    nominatedStudent.setBic(rs.getString(21));
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(rs.getInt(22));
    nominatedStudent.setAddress(address);
    nominatedStudent.setVersion(rs.getInt(23));
    return nominatedStudent;
  }
}
