package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.PartnerDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link PartnerDao} with Spring's {@link JdbcClient}. Partners are read joined with their
 * address, country, programme and options; the inner join on the options makes a partner without
 * any option invisible (legacy behaviour pinned by PartnerDaoIT). Updates check the version.
 */
@Repository
class PartnerDaoImpl implements PartnerDao {

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.partners
        (legal_name, business_name, full_name, organisation_type, employee_count, address, email,
         website, phone_number, is_official, is_archived, version)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, 1)
      RETURNING partner_id""";

  private static final String SQL_SELECT = """
      SELECT DISTINCT p.partner_id, p.legal_name, p.business_name, p.full_name,
             p.organisation_type, p.employee_count, p.address, p.email, p.website, p.phone_number,
             p.is_official, p.is_archived, p.version, pr.programme_id, pr.name, c.country_code,
             c.name
        FROM student_exchange_tools.partners p
        JOIN student_exchange_tools.addresses a ON p.address = a.address_id
        JOIN student_exchange_tools.countries c ON a.country = c.country_code
        JOIN student_exchange_tools.programmes pr ON c.programme_id = pr.programme_id
        JOIN student_exchange_tools.partner_options po ON po.partner_id = p.partner_id
        JOIN student_exchange_tools.options o ON po.option_code = o.option_code
       WHERE TRUE""";

  private static final String SQL_UPDATE = """
      UPDATE student_exchange_tools.partners p
         SET (legal_name, business_name, full_name, organisation_type, employee_count, address,
              email, website, phone_number, is_official, is_archived, version)
           = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version + 1)
       WHERE p.partner_id = ? AND p.version = ?
      RETURNING p.version""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  PartnerDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public PartnerDto create(PartnerDto partner) {
    int id = DataAccess.call(() -> bindColumns(jdbcClient.sql(SQL_INSERT), partner)
        .query(Integer.class).single());
    partner.setId(id);
    partner.setVersion(1);
    return partner;
  }

  @Override
  public PartnerDto findById(int id) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " AND p.partner_id = ?").param(id)
        .query(this::toDto).list().stream().findFirst().orElse(null));
  }

  /**
   * {@inheritDoc}
   *
   * <p>As before, a student only sees the official, non-archived partners of their option, and
   * an unknown filter adds no condition but still binds {@code %value%}, which the database
   * rejects (FatalException).
   */
  @Override
  public List<PartnerDto> findAll(String filter, String value, String userRole, String option) {
    boolean student = userRole.equals(UserDto.ROLE_STUDENT);
    String condition = "";
    List<Object> params = new ArrayList<>();
    if (filter.equals(FILTER_ALL_PARTNERS)) {
      if (student) {
        condition = " AND p.is_archived = FALSE AND p.is_official = TRUE AND po.option_code = ?";
        params.add(option);
      }
    } else if (filter.equals(FILTER_COUNTRY)) {
      condition = " AND c.country_code = ?";
      params.add(value);
      if (student) {
        condition += " AND p.is_archived = FALSE AND po.option_code = ? AND p.is_official = TRUE";
        params.add(option);
      }
    } else {
      if (filter.equals(FILTER_ARCHIVED_PARTNERS)) {
        condition = " AND p.is_archived = TRUE AND lower(p.full_name) LIKE ?";
      }
      params.add("%" + value.toLowerCase() + "%");
    }
    String sql = SQL_SELECT + condition;
    return DataAccess.call(() -> jdbcClient.sql(sql).params(params).query(this::toDto).list());
  }

  @Override
  public PartnerDto update(PartnerDto partner) {
    int version = DataAccess.call(() -> bindColumns(jdbcClient.sql(SQL_UPDATE), partner)
        .param(partner.isArchived()).param(partner.getId()).param(partner.getVersion())
        .query(Integer.class).optional().orElseThrow(ConcurrentModificationException::new));
    partner.setVersion(version);
    return partner;
  }

  /** Binds the columns legal_name to is_official, in the order of the statements above. */
  private static JdbcClient.StatementSpec bindColumns(JdbcClient.StatementSpec statement,
      PartnerDto partner) {
    return statement.param(partner.getLegalName())
        .param(partner.getBusinessName())
        .param(partner.getFullName())
        .param(partner.getOrganisationType())
        .param(partner.getEmployeeCount())
        .param(partner.getAddress().getId())
        .param(partner.getEmail())
        .param(partner.getWebsite())
        .param(partner.getPhoneNumber())
        .param(partner.isOfficial());
  }

  private PartnerDto toDto(ResultSet rs, int rowNum) throws SQLException {
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
