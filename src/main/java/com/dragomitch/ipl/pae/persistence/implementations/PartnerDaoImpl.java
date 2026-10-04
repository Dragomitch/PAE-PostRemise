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
import com.dragomitch.ipl.pae.persistence.PartnerDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link PartnerDao} with Spring's {@link JdbcClient}. Partners are read joined with their
 * address, country and programme, and LEFT-joined with their options: a partner without any
 * option is still found (with an empty option list), and the rows of a partner with several
 * options are merged into one DTO by {@link #readPartners(ResultSet)}. Updates check the version.
 */
@Repository
class PartnerDaoImpl implements PartnerDao {

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.partners
        (legal_name, business_name, full_name, organisation_type, employee_count, address, email,
         website, phone_number, is_official, is_archived, version)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, 1)
      RETURNING partner_id""";

  /** Partners with one row per option, ordered by {@link #SQL_ORDER}: see {@link #readPartners}. */
  private static final String SQL_SELECT = """
      SELECT p.partner_id, p.legal_name, p.business_name, p.full_name,
             p.organisation_type, p.employee_count, p.address, p.email, p.website, p.phone_number,
             p.is_official, p.is_archived, p.version, pr.programme_id, pr.name, c.country_code,
             c.name, po.option_code, po.departement, o.name
        FROM student_exchange_tools.partners p
        JOIN student_exchange_tools.addresses a ON p.address = a.address_id
        JOIN student_exchange_tools.countries c ON a.country = c.country_code
        JOIN student_exchange_tools.programmes pr ON c.programme_id = pr.programme_id
        LEFT JOIN student_exchange_tools.partner_options po ON po.partner_id = p.partner_id
        LEFT JOIN student_exchange_tools.options o ON o.option_code = po.option_code""";

  private static final String SQL_ORDER = " ORDER BY p.partner_id, po.option_code";

  /**
   * Restricts a query to the partners offering the option bound to the parameter, without
   * restricting the options read for them (hence an EXISTS rather than a condition on
   * {@code po}).
   */
  private static final String HAS_OPTION = """
      EXISTS (SELECT 1 FROM student_exchange_tools.partner_options f
               WHERE f.partner_id = p.partner_id AND f.option_code = ?)""";

  private static final String SQL_UPDATE = """
      UPDATE student_exchange_tools.partners p
         SET (legal_name, business_name, full_name, organisation_type, employee_count, address,
              email, website, phone_number, is_official, is_archived, version)
           = (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, version + 1)
       WHERE p.partner_id = ? AND p.version = ?
      RETURNING p.version""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;
  private final ResultSetExtractor<List<PartnerDto>> partnersReader = this::readPartners;

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
    return DataAccess.call(() -> jdbcClient
        .sql(SQL_SELECT + " WHERE p.partner_id = ?" + SQL_ORDER).param(id)
        .query(partnersReader).stream().findFirst().orElse(null));
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
    List<String> conditions = new ArrayList<>();
    List<Object> params = new ArrayList<>();
    if (FILTER_COUNTRY.equals(filter)) {
      conditions.add("c.country_code = ?");
      params.add(value);
    } else if (FILTER_ARCHIVED_PARTNERS.equals(filter)) {
      conditions.add("p.is_archived = TRUE");
      conditions.add("lower(p.full_name) LIKE ?");
      params.add("%" + value.toLowerCase() + "%");
    } else if (!FILTER_ALL_PARTNERS.equals(filter)) {
      throw new BusinessException(ErrorFormat.INVALID_PARTNER_FILTER_709);
    }
    if (student && !FILTER_ARCHIVED_PARTNERS.equals(filter)) {
      conditions.add("p.is_archived = FALSE");
      conditions.add("p.is_official = TRUE");
      conditions.add(HAS_OPTION);
      params.add(option);
    }
    String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    String sql = SQL_SELECT + where + SQL_ORDER;
    return DataAccess.call(() -> jdbcClient.sql(sql).params(params).query(partnersReader));
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

  /**
   * Reads the rows of {@link #SQL_SELECT} (ordered by partner): one partner per id, with the
   * options of its rows.
   *
   * @param rs the rows
   * @return the partners, in the order of the rows
   */
  private List<PartnerDto> readPartners(ResultSet rs) throws SQLException {
    Map<Integer, PartnerDto> partners = new LinkedHashMap<>();
    while (rs.next()) {
      PartnerDto partner = partners.get(rs.getInt(1));
      if (partner == null) {
        partner = toDto(rs);
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
    return new ArrayList<>(partners.values());
  }

  private PartnerDto toDto(ResultSet rs) throws SQLException {
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
