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
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link MobilityChoiceDao} with Spring's {@link JdbcClient}. Mobility choices are read joined
 * with the student and their option, the programme and, when set, the country, the denial reason
 * and the partner. The filters of {@link #findAll(String)} and the "active" queries compare the
 * academic year with the current year. Updates check the version.
 */
@Repository
class MobilityChoiceDaoImpl implements MobilityChoiceDao {

  private static final String SQL_SELECT = """
      SELECT mc.mobility_choice_id, mc.user_id, u.last_name, u.first_name, op.option_code,
             op.name, mc.preference_order, mc.mobility_type, mc.academic_year, mc.term,
             mc.programme, p.name, mc.country, mc.submission_date, mc.prof_denial_reason,
             mc.student_cancellation_reason, mc.partner, pa.full_name, mc.version
        FROM student_exchange_tools.mobility_choices mc
        JOIN student_exchange_tools.users u ON u.user_id = mc.user_id
        JOIN student_exchange_tools.options op ON u.option = op.option_code
        JOIN student_exchange_tools.programmes p ON p.programme_id = mc.programme
        LEFT OUTER JOIN student_exchange_tools.countries c ON mc.country = c.country_code
        LEFT OUTER JOIN student_exchange_tools.denial_reasons dr
          ON dr.reason_id = mc.prof_denial_reason
        LEFT OUTER JOIN student_exchange_tools.partners pa ON pa.partner_id = mc.partner
        LEFT OUTER JOIN student_exchange_tools.mobilities m
          ON m.mobility_choice_id = mc.mobility_choice_id
       WHERE TRUE""";

  /** Choices that did not become a mobility. */
  private static final String NOT_A_MOBILITY = " AND mc.mobility_choice_id NOT IN"
      + " (SELECT m.mobility_choice_id FROM student_exchange_tools.mobilities m)";

  /**
   * Current-year choices of a partner, neither denied nor cancelled, excluding those whose
   * mobility is both denied and cancelled.
   */
  private static final String ACTIVE_FOR_PARTNER = " AND mc.partner = ? AND mc.academic_year = ?"
      + " AND mc.prof_denial_reason IS NULL AND mc.student_cancellation_reason IS NULL"
      + " AND mc.mobility_choice_id NOT IN (SELECT m.mobility_choice_id"
      + " FROM student_exchange_tools.mobilities m WHERE m.prof_denial_reason IS NOT NULL"
      + " AND m.student_cancellation_reason IS NOT NULL)";

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.mobility_choices
        (user_id, preference_order, mobility_type, academic_year, term, programme, country,
         submission_date, prof_denial_reason, student_cancellation_reason, partner, version)
      VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, 1)
      RETURNING mobility_choice_id, submission_date""";

  private static final String SQL_UPDATE = """
      UPDATE student_exchange_tools.mobility_choices mc
         SET (preference_order, mobility_type, academic_year, term, programme, country,
              submission_date, prof_denial_reason, student_cancellation_reason, partner, version)
           = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version + 1)
       WHERE mc.mobility_choice_id = ? AND mc.version = ?
      RETURNING version""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  MobilityChoiceDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public MobilityChoiceDto create(MobilityChoiceDto mobilityChoice) {
    CountryDto country = mobilityChoice.getCountry();
    DenialReasonDto denialReason = mobilityChoice.getDenialReason();
    PartnerDto partner = mobilityChoice.getPartner();
    // -1 is the "no partner" of the web UI
    Integer partnerId = partner != null && partner.getId() != -1 ? partner.getId() : null;
    DataAccess.run(() -> jdbcClient.sql(SQL_INSERT)
        .param(mobilityChoice.getUser().getId())
        .param(mobilityChoice.getPreferenceOrder())
        .param(mobilityChoice.getMobilityType())
        .param(mobilityChoice.getAcademicYear())
        .param(mobilityChoice.getTerm())
        .param(mobilityChoice.getProgramme().getId())
        .param(DataAccess.typed(Types.VARCHAR, country == null ? null : country.getCountryCode()))
        .param(DataAccess.typed(Types.INTEGER, denialReason == null ? null : denialReason.getId()))
        .param(DataAccess.typed(Types.VARCHAR, mobilityChoice.getCancellationReason()))
        .param(DataAccess.typed(Types.INTEGER, partnerId))
        .query(rs -> {
          mobilityChoice.setId(rs.getInt(1));
          mobilityChoice.setSubmissionDate(rs.getTimestamp(2).toLocalDateTime());
        }));
    mobilityChoice.setVersion(1);
    return mobilityChoice;
  }

  @Override
  public MobilityChoiceDto findById(int mobilityChoiceId) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " AND mc.mobility_choice_id = ?")
        .param(mobilityChoiceId).query(this::toDto).list().stream().findFirst().orElse(null));
  }

  @Override
  public List<MobilityChoiceDto> findAll(String filter) {
    String condition = "";
    boolean currentYear = false;
    if (filter.equals(FILTER_CANCELED_MOBILITIES_CHOICES)) {
      condition = " AND mc.student_cancellation_reason IS NOT NULL";
    } else if (filter.equals(FILTER_REJECTED_MOBILITIES_CHOICES)) {
      condition = " AND mc.prof_denial_reason IS NOT NULL";
    } else if (filter.equals(FILTER_PASSED_MOBILITIES_CHOICES)) {
      condition = " AND mc.academic_year < ?";
      currentYear = true;
    } else if (filter.equals(FILTER_ACTIVE_MOBILITIES_CHOICES)) {
      condition = " AND mc.academic_year = ? AND mc.prof_denial_reason IS NULL"
          + " AND mc.student_cancellation_reason IS NULL" + NOT_A_MOBILITY;
      currentYear = true;
    }
    JdbcClient.StatementSpec statement = jdbcClient.sql(SQL_SELECT + condition);
    JdbcClient.StatementSpec query =
        currentYear ? statement.param(LocalDate.now().getYear()) : statement;
    return DataAccess.call(() -> query.query(this::toDto).list());
  }

  @Override
  public void update(MobilityChoiceDto mobilityChoice) {
    DenialReasonDto denialReason = mobilityChoice.getDenialReason();
    String cancellationReason = isAValidString(mobilityChoice.getCancellationReason())
        ? mobilityChoice.getCancellationReason() : null;
    PartnerDto partner = mobilityChoice.getPartner();
    int version = DataAccess.call(() -> jdbcClient.sql(SQL_UPDATE)
        .param(mobilityChoice.getPreferenceOrder())
        .param(mobilityChoice.getMobilityType())
        .param(mobilityChoice.getAcademicYear())
        .param(mobilityChoice.getTerm())
        .param(mobilityChoice.getProgramme().getId())
        .param(mobilityChoice.getCountry().getCountryCode())
        .param(Timestamp.valueOf(mobilityChoice.getSubmissionDate()))
        .param(DataAccess.typed(Types.INTEGER, denialReason == null ? null : denialReason.getId()))
        .param(DataAccess.typed(Types.VARCHAR, cancellationReason))
        .param(DataAccess.typed(Types.INTEGER, partner == null ? null : partner.getId()))
        .param(mobilityChoice.getId())
        .param(mobilityChoice.getVersion())
        .query(Integer.class).optional().orElse(0));
    if (version == 0) {
      throw new ConcurrentModificationException("The data have been modified before that query");
    }
  }

  @Override
  public List<MobilityChoiceDto> findByUser(int userId) {
    return findNotYetMobilityBy("mc.user_id", userId);
  }

  @Override
  public List<MobilityChoiceDto> findByPartner(int partnerId) {
    return findNotYetMobilityBy("mc.partner", partnerId);
  }

  @Override
  public List<MobilityChoiceDto> findByActivePartner(int partnerId) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + ACTIVE_FOR_PARTNER)
        .param(partnerId).param(LocalDate.now().getYear()).query(this::toDto).list());
  }

  /**
   * The choices, not (yet) a mobility, whose column has the given value.
   *
   * @param column the qualified column (a constant of this class, never user input)
   * @param value its value
   * @return the matching choices
   */
  private List<MobilityChoiceDto> findNotYetMobilityBy(String column, int value) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " AND " + column + " = ?"
        + NOT_A_MOBILITY).param(value).query(this::toDto).list());
  }

  private MobilityChoiceDto toDto(ResultSet rs, int rowNum) throws SQLException {
    MobilityChoiceDto mobilityChoice =
        (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    mobilityChoice.setId(rs.getInt(1));
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
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
    int denialReasonId = rs.getInt(15);
    if (denialReasonId > 0) {
      DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
      denialReason.setId(denialReasonId);
      mobilityChoice.setDenialReason(denialReason);
    } else {
      mobilityChoice.setDenialReason(null);
    }
    mobilityChoice.setCancellationReason(rs.getString(16));
    int partnerId = rs.getInt(17);
    if (partnerId > 0) {
      PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
      partner.setId(partnerId);
      partner.setFullName(rs.getString(18));
      mobilityChoice.setPartner(partner);
    } else {
      mobilityChoice.setPartner(null);
    }
    mobilityChoice.setVersion(rs.getInt(19));
    return mobilityChoice;
  }
}
