package persistence.implementations;

import business.EntityFactory;
import business.dto.CountryDto;
import business.dto.PartnerDto;
import business.dto.PaymentDto;
import business.dto.ProgrammeDto;
import business.dto.UserDto;
import main.ContextManager;
import main.annotations.Inject;
import main.exceptions.FatalException;
import persistence.PaymentDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PaymentDaoImpl implements PaymentDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_SELECT = "SELECT mc." + COLUMN_MOBILITY_CHOICE_ID + ", mc."
      + COLUMN_USER_ID + ", u." + COLUMN_FIRST_NAME + ", u." + COLUMN_LAST_NAME + ", mc."
      + COLUMN_MOBILITY_TYPE + ", mc." + COLUMN_ACADEMIC_YEAR + ", mc." + COLUMN_TERM + ", mc."
      + COLUMN_PROGRAMME + ", p." + COLUMN_NAME + ", mc." + COLUMN_COUNTRY + ", c." + COLUMN_NAME
      + ", mc." + COLUMN_PARTNER + ", pa." + COLUMN_FULL_NAME + ", m."
      + COLUMN_FIRST_PAYMENT_REQUEST_DATE + " AS payment_date, 'D' AS \"payment_type\" FROM "
      + SCHEMA_NAME + "." + TABLE_MOBILITY_CHOICES_NAME + " mc, " + SCHEMA_NAME + "."
      + TABLE_USERS_NAME + " u, " + SCHEMA_NAME + "." + TABLE_MOBILITIES_NAME + " m, " + SCHEMA_NAME
      + "." + TABLE_PROGRAMMES_NAME + " p, " + SCHEMA_NAME + "." + TABLE_COUNTRIES_NAME + " c, "
      + SCHEMA_NAME + "." + TABLE_PARTNERS_NAME + " pa WHERE mc." + COLUMN_USER_ID + " = u."
      + COLUMN_USER_ID + " AND mc." + COLUMN_MOBILITY_CHOICE_ID + " = m."
      + COLUMN_MOBILITY_CHOICE_ID + " AND mc." + COLUMN_PROGRAMME + " = p." + COLUMN_PROGRAMME_ID
      + " AND mc." + COLUMN_COUNTRY + " = c." + COLUMN_COUNTRY_CODE + " AND mc." + COLUMN_PARTNER
      + " = pa." + COLUMN_PARTNER_ID + " AND m." + COLUMN_FIRST_PAYMENT_REQUEST_DATE
      + " IS NOT NULL UNION SELECT mc." + COLUMN_MOBILITY_CHOICE_ID + ", mc." + COLUMN_USER_ID
      + ", u." + COLUMN_FIRST_NAME + ", u." + COLUMN_LAST_NAME + ", mc." + COLUMN_MOBILITY_TYPE
      + ", mc." + COLUMN_ACADEMIC_YEAR + ", mc." + COLUMN_TERM + ", mc." + COLUMN_PROGRAMME + ", p."
      + COLUMN_NAME + ", mc." + COLUMN_COUNTRY + ", c." + COLUMN_NAME + ", mc." + COLUMN_PARTNER
      + ", pa." + COLUMN_FULL_NAME + ", m." + COLUMN_SECOND_PAYMENT_REQUEST_DATE
      + " AS payment_date, 'R' AS \"payment_type\" FROM " + SCHEMA_NAME + "."
      + TABLE_MOBILITY_CHOICES_NAME + " mc, " + SCHEMA_NAME + "." + TABLE_USERS_NAME + " u, "
      + SCHEMA_NAME + "." + TABLE_MOBILITIES_NAME + " m, " + SCHEMA_NAME + "."
      + TABLE_PROGRAMMES_NAME + " p, " + SCHEMA_NAME + "." + TABLE_COUNTRIES_NAME + " c, "
      + SCHEMA_NAME + "." + TABLE_PARTNERS_NAME + " pa WHERE mc." + COLUMN_USER_ID + " = u."
      + COLUMN_USER_ID + " AND mc." + COLUMN_MOBILITY_CHOICE_ID + " = m."
      + COLUMN_MOBILITY_CHOICE_ID + " AND mc." + COLUMN_PROGRAMME + " = p." + COLUMN_PROGRAMME_ID
      + " AND mc." + COLUMN_COUNTRY + " = c." + COLUMN_COUNTRY_CODE + " AND mc." + COLUMN_PARTNER
      + " = pa." + COLUMN_PARTNER_ID + " AND m." + COLUMN_SECOND_PAYMENT_REQUEST_DATE
      + " IS NOT NULL";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser.
   * @param dalBackendServices backend services.
   */
  @Inject
  public PaymentDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public List<PaymentDto> findAll() {
    List<PaymentDto> payments = new ArrayList<PaymentDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          payments.add(populateDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return payments;
  }

  private PaymentDto populateDto(ResultSet rs) throws SQLException {
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
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(rs.getString(10));
    country.setName(rs.getString(11));
    payment.setCountry(country);
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(rs.getInt(12));
    partner.setFullName(rs.getString(13));
    payment.setPartner(partner);
    payment.setPaymentDate(rs.getTimestamp(14).toLocalDateTime());
    payment.setPaymentType(rs.getString(15));
    return payment;
  }


}
