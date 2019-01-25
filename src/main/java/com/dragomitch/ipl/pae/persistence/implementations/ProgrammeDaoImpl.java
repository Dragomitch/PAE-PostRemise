package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProgrammeDaoImpl implements ProgrammeDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SQL_SELECT = "SELECT pr." + COLUMN_ID + ", pr." + COLUMN_NAME
      + ", pr." + COLUMN_EXTERNAL_SOFTWARE_NAME + " FROM " + SCHEMA_NAME + "." + TABLE_NAME + " pr";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public ProgrammeDaoImpl(EntityFactory entityFactory, DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public ProgrammeDto findById(int id) {
    ProgrammeDto programme = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " WHERE pr." + COLUMN_ID + "= ?")) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          programme = populateProgrammeDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return programme;
  }

  /**
   * Populates a ProgrammeDto object from a row of data.
   * 
   * @param rs : a cursor pointing to its current row of data
   * @return programme : an instance of ProgrammeDto built based on a row of data
   */
  private ProgrammeDto populateProgrammeDto(ResultSet rs) throws SQLException {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(rs.getInt(1));
    programme.setProgrammeName(rs.getString(2));
    programme.setExternalSoftName(rs.getString(3));
    return programme;
  }

  @Override
  public List<ProgrammeDto> findAll() {
    List<ProgrammeDto> programmes = new ArrayList<ProgrammeDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          programmes.add(populateProgrammeDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return programmes;
  }


}
