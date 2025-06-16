package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import org.springframework.stereotype.Repository;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.OptionDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
class OptionDaoImpl implements OptionDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_SELECT = "SELECT o." + COLUMN_CODE + ", o." + COLUMN_NAME
      + " FROM " + SCHEMA_NAME + "." + TABLE_NAME + " o";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  @Inject
  public OptionDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public OptionDto findByCode(String code) {
    OptionDto option = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " WHERE " + COLUMN_CODE + " = ?")) {
      stmt.setString(1, code);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          option = populateOptionDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return option;
  }

  @Override
  public List<OptionDto> findAll() {
    List<OptionDto> options = new ArrayList<OptionDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          options.add(populateOptionDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return options;
  }

  private OptionDto populateOptionDto(ResultSet rs) throws SQLException {
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(rs.getString(1));
    option.setName(rs.getString(2));
    return option;
  }

}
