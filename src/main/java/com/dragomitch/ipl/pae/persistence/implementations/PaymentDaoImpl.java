package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.PaymentDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link PaymentDao} with Spring's {@link JdbcClient}. Payments are not a table but a read model
 * over mobility choices and mobilities (one row per requested payment: 'D' for the first request,
 * 'R' for the second), so there is no aggregate to map: the query stays plain SQL. The choice may
 * have no country and no partner: they are LEFT-joined and stay null in the DTO. JdbcClient
 * runs on the connection of the current transaction, like the Spring Data repositories.
 */
@Repository
class PaymentDaoImpl implements PaymentDao {

  private static final String SQL_SELECT = """
      SELECT mc.mobility_choice_id, mc.user_id, u.first_name, u.last_name, mc.mobility_type,
             mc.academic_year, mc.term, mc.programme, p.name, mc.country, c.name, mc.partner,
             pa.full_name, m.%1$s_payment_request_date AS payment_date, '%2$s' AS payment_type
        FROM student_exchange_tools.mobility_choices mc
        JOIN student_exchange_tools.users u ON mc.user_id = u.user_id
        JOIN student_exchange_tools.mobilities m ON mc.mobility_choice_id = m.mobility_choice_id
        JOIN student_exchange_tools.programmes p ON mc.programme = p.programme_id
        LEFT JOIN student_exchange_tools.countries c ON mc.country = c.country_code
        LEFT JOIN student_exchange_tools.partners pa ON mc.partner = pa.partner_id
       WHERE m.%1$s_payment_request_date IS NOT NULL
      """;

  private static final String SQL_PAYMENTS =
      SQL_SELECT.formatted("first", "D") + " UNION " + SQL_SELECT.formatted("second", "R");

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  PaymentDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public List<PaymentDto> findAll() {
    return DataAccess.call(() -> jdbcClient.sql(SQL_PAYMENTS).query(this::toDto).list());
  }

  private PaymentDto toDto(ResultSet rs, int rowNum) throws SQLException {
    PaymentDto payment = (PaymentDto) entityFactory.build(PaymentDto.class);
    payment.setMobilityChoiceId(rs.getInt(1));
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(rs.getInt(2));
    user.setFirstName(rs.getString(3));
    user.setLastName(rs.getString(4));
    payment.setUser(user);
    payment.setMobilityType(rs.getString(5));
    payment.setAcademicYear(rs.getString(6));
    payment.setTerm(rs.getInt(7));
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(rs.getInt(8));
    programme.setProgrammeName(rs.getString(9));
    payment.setProgramme(programme);
    String countryCode = rs.getString(10);
    if (countryCode != null) {
      CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
      country.setCountryCode(countryCode);
      country.setName(rs.getString(11));
      payment.setCountry(country);
    }
    int partnerId = rs.getInt(12);
    if (!rs.wasNull()) {
      PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
      partner.setId(partnerId);
      partner.setFullName(rs.getString(13));
      payment.setPartner(partner);
    }
    payment.setPaymentDate(rs.getTimestamp(14).toLocalDateTime());
    payment.setPaymentType(rs.getString(15));
    return payment;
  }
}
