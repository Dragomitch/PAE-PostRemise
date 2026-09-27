package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.MobilityDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link MobilityDao} with Spring's {@link JdbcClient}. A mobility extends an accepted mobility
 * choice (same id) and is read joined with the choice, the student, the partner, the country, the
 * programme and the optional denial reason. Updates check the version.
 */
@Repository
class MobilityDaoImpl implements MobilityDao {

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.mobilities
        (mobility_choice_id, submission_date, state, state_before_cancellation,
         first_payment_request_date, second_payment_request_date, pro_eco_encoding,
         second_software_encoding, student_cancellation_reason, prof_denial_reason,
         professor_in_charge, version)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)""";

  private static final String SQL_SELECT = """
      SELECT mc.mobility_choice_id, mc.mobility_type, mc.academic_year, mc.term,
             m.submission_date, m.state, m.state_before_cancellation,
             m.first_payment_request_date, m.second_payment_request_date, m.pro_eco_encoding,
             m.second_software_encoding, m.student_cancellation_reason, m.prof_denial_reason,
             dr.reason, m.professor_in_charge, m.version, mc.user_id, u.first_name, u.last_name,
             u.option, pa.partner_id, pa.full_name, c.country_code, c.name, p.programme_id, p.name
        FROM student_exchange_tools.mobility_choices mc
        JOIN student_exchange_tools.mobilities m ON mc.mobility_choice_id = m.mobility_choice_id
        JOIN student_exchange_tools.users u ON mc.user_id = u.user_id
        JOIN student_exchange_tools.partners pa ON mc.partner = pa.partner_id
        JOIN student_exchange_tools.countries c ON mc.country = c.country_code
        JOIN student_exchange_tools.programmes p ON mc.programme = p.programme_id
        LEFT OUTER JOIN student_exchange_tools.denial_reasons dr
          ON m.prof_denial_reason = dr.reason_id""";

  private static final String SQL_UPDATE = """
      UPDATE student_exchange_tools.mobilities
         SET (submission_date, state, state_before_cancellation, first_payment_request_date,
              second_payment_request_date, pro_eco_encoding, second_software_encoding,
              student_cancellation_reason, prof_denial_reason, professor_in_charge, version)
           = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version + 1)
       WHERE mobility_choice_id = ? AND version = ?
      RETURNING version""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  MobilityDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public MobilityDto create(MobilityDto mobility) {
    DataAccess.run(() -> bindColumns(jdbcClient.sql(SQL_INSERT).param(mobility.getId()), mobility)
        .update());
    mobility.setVersion(1);
    return mobility;
  }

  @Override
  public MobilityDto findById(int id) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " WHERE m.mobility_choice_id = ?")
        .param(id).query(this::toDto).list().stream().findFirst().orElse(null));
  }

  @Override
  public List<MobilityDto> findAll() {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT).query(this::toDto).list());
  }

  @Override
  public List<MobilityDto> findByUser(int user) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " WHERE u.user_id = ?").param(user)
        .query(this::toDto).list());
  }

  @Override
  public MobilityDto update(MobilityDto mobility) {
    int version = DataAccess.call(() -> bindColumns(jdbcClient.sql(SQL_UPDATE), mobility)
        .param(mobility.getId()).param(mobility.getVersion())
        .query(Integer.class).optional().orElseThrow(ConcurrentModificationException::new));
    mobility.setVersion(version);
    return mobility;
  }

  /** Binds the columns submission_date to professor_in_charge, in the statements' order. */
  private static JdbcClient.StatementSpec bindColumns(JdbcClient.StatementSpec statement,
      MobilityDto mobility) {
    DenialReasonDto denialReason = mobility.getDenialReason();
    UserDto professor = mobility.getProfessorInCharge();
    return statement.param(Timestamp.valueOf(mobility.getSubmissionDate()))
        .param(mobility.getState())
        .param(DataAccess.typed(Types.VARCHAR, mobility.getStateBeforeCancellation()))
        .param(timestamp(mobility.getFirstPaymentRequestDate()))
        .param(timestamp(mobility.getSecondPaymentRequestDate()))
        .param(mobility.isEncodedInProEco())
        .param(mobility.isEncodedInSecondSoftware())
        .param(DataAccess.typed(Types.VARCHAR, mobility.getCancellationReason()))
        .param(DataAccess.typed(Types.INTEGER, denialReason == null ? null : denialReason.getId()))
        .param(DataAccess.typed(Types.INTEGER, professor == null ? null : professor.getId()));
  }

  private static SqlParameterValue timestamp(LocalDateTime dateTime) {
    return DataAccess.typed(Types.TIMESTAMP, dateTime == null ? null : Timestamp.valueOf(dateTime));
  }

  private MobilityDto toDto(ResultSet rs, int rowNum) throws SQLException {
    MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
    mobility.setId(rs.getInt(1));
    mobility.setMobilityType(rs.getString(2));
    mobility.setAcademicYear(rs.getInt(3));
    mobility.setTerm(rs.getInt(4));
    mobility.setSubmissionDate(rs.getTimestamp(5).toLocalDateTime());
    mobility.setState(rs.getString(6));
    mobility.setStateBeforeCancellation(rs.getString(7));
    Timestamp firstPayment = rs.getTimestamp(8);
    if (firstPayment != null) {
      mobility.setFirstPaymentRequestDate(firstPayment.toLocalDateTime());
    }
    Timestamp secondPayment = rs.getTimestamp(9);
    if (secondPayment != null) {
      mobility.setSecondPaymentRequestDate(secondPayment.toLocalDateTime());
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
