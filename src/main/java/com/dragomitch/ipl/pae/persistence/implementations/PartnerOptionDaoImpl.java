package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link PartnerOptionDao} with Spring's {@link JdbcClient}: the options (and department) a
 * partner welcomes students from (composite key option_code + partner_id).
 */
@Repository
class PartnerOptionDaoImpl implements PartnerOptionDao {

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.partner_options (option_code, partner_id, departement)
      VALUES (?, ?, ?)""";

  private static final String SQL_SELECT = """
      SELECT po.option_code, po.partner_id, po.departement, o.name
        FROM student_exchange_tools.partner_options po
        JOIN student_exchange_tools.options o ON o.option_code = po.option_code""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  PartnerOptionDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public PartnerOptionDto create(PartnerOptionDto partnerOption, int partnerId) {
    DataAccess.run(() -> jdbcClient.sql(SQL_INSERT).param(partnerOption.getCode())
        .param(partnerId).param(partnerOption.getDepartement()).update());
    return partnerOption;
  }

  @Override
  public List<PartnerDto> findAllPartnersByOption(String optionCode) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " WHERE po.option_code = ?")
        .param(optionCode).query(this::toPartnerDto).list());
  }

  @Override
  public List<PartnerOptionDto> findAllOptionsByPartner(int partnerId) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT + " WHERE po.partner_id = ?")
        .param(partnerId).query(this::toPartnerOptionDto).list());
  }

  /** The option code, its name and the department; the partner is the one queried. */
  private PartnerOptionDto toPartnerOptionDto(ResultSet rs, int rowNum) throws SQLException {
    PartnerOptionDto partnerOption = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    partnerOption.setCode(rs.getString(1));
    partnerOption.setDepartement(rs.getString(3));
    partnerOption.setName(rs.getString(4));
    return partnerOption;
  }

  /** The partner id only. */
  private PartnerDto toPartnerDto(ResultSet rs, int rowNum) throws SQLException {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(rs.getInt(2));
    return partner;
  }
}
