package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class PartnerOptionDaoImpl implements PartnerOptionDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);


  private static final String SQL_INSERT =
      "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + " (" + COLUMN_OPTION_CODE + ", "
          + COLUMN_PARTNER_ID + ", " + COLUMN_DEPARTEMENT + ") VALUES (?, ?, ?)";

  private static final String SQL_SELECT =
      "SELECT po." + COLUMN_OPTION_CODE + ", po." + COLUMN_PARTNER_ID + ", po." + COLUMN_DEPARTEMENT
          + " FROM " + SCHEMA_NAME + "." + TABLE_NAME + " po";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;


  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public PartnerOptionDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public PartnerOptionDto create(PartnerOptionDto partnerOption, int partnerId) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_INSERT)) {
      stmt.setString(1, partnerOption.getCode());
      stmt.setInt(2, partnerId);
      stmt.setString(3, partnerOption.getDepartement());
      stmt.execute();
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG);
    }
    return partnerOption;
  }

  @Override
  public List<PartnerDto> findAllPartnersByOption(String optionCode) {
    List<PartnerDto> partners = new ArrayList<PartnerDto>();
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " WHERE po.option_code = ?")) {
      stmt.setString(1, optionCode);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          partners.add(populatePartnerDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return partners;
  }

  @Override
  public List<PartnerOptionDto> findAllOptionsByPartner(int partnerId) {
    List<PartnerOptionDto> partnerOptions = new ArrayList<PartnerOptionDto>();
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " WHERE po.partner_id = ?")) {
      stmt.setInt(1, partnerId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          partnerOptions.add(populatePartnerOptionDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return partnerOptions;
  }

  /*
   * /** Populate an OptionDto based on a resultSet.
   * 
   * @param rs a cursor pointing to its current row of data
   * 
   * @return an optionDto
   */
  private PartnerOptionDto populatePartnerOptionDto(ResultSet rs) throws SQLException {
    PartnerOptionDto partnerOption = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    partnerOption.setCode(rs.getString(1));
    partnerOption.setDepartement(rs.getString(3));
    return partnerOption;
  }

  /**
   * Populate a PartnerDto based on a resultSet.
   * 
   * @param rs a cursor pointing to its current row of data
   * @return a partnerDto
   */
  private PartnerDto populatePartnerDto(ResultSet rs) throws SQLException {
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(rs.getInt(2));
    return partner;
  }
}
