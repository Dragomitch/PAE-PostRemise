package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import org.springframework.stereotype.Repository;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
class DenialReasonDaoImpl implements DenialReasonDao {

  private static final String SCHEMA_NAME = "student_exchange_tools";

  private static final String SQL_INSERT =
      "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + "(reason) VALUES (?) RETURNING reason_id";

  private static final String SQL_SELECT =
      "SELECT " + COLUMN_ID + ", " + COLUMN_REASON + " FROM " + SCHEMA_NAME + "." + TABLE_NAME;

  private static final String SQL_UPDATE = "UPDATE " + SCHEMA_NAME + "." + TABLE_NAME + " SET "
      + COLUMN_REASON + " = ? WHERE " + COLUMN_ID + " = ?";

  private final DtoFactory dtoFactory;
  private final DalBackendServices dalBackendServices;

  
  public DenialReasonDaoImpl(DtoFactory dtoFactory, DalBackendServices dalBackendServices) {
    this.dtoFactory = dtoFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public DenialReasonDto create(DenialReasonDto denialReason) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_INSERT)) {
      stmt.setString(1, denialReason.getReason());
      try (ResultSet rs = stmt.executeQuery()) {
        rs.next();
        denialReason.setId(rs.getInt(1));
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return denialReason;
  }

  @Override
  public DenialReasonDto findById(int id) {
    DenialReasonDto denialReason = null;
    try (PreparedStatement stmt =
        dalBackendServices.prepareStatement(SQL_SELECT + " WHERE reason_id = ?")) {
      stmt.setInt(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          denialReason = populateDto(rs);
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return denialReason;
  }

  @Override
  public List<DenialReasonDto> findAll() {
    List<DenialReasonDto> reasons = new ArrayList<DenialReasonDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_SELECT)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          reasons.add(populateDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return reasons;
  }

  @Override
  public DenialReasonDto update(DenialReasonDto denialReason) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SQL_UPDATE)) {
      stmt.setString(1, denialReason.getReason());
      stmt.setInt(2, denialReason.getId());
      stmt.execute();
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return denialReason;
  }

  private DenialReasonDto populateDto(ResultSet set) throws SQLException {
    DenialReasonDto reason = (DenialReasonDto) dtoFactory.create(DenialReasonDto.class);
    reason.setId(set.getInt(1));
    reason.setReason(set.getString(2));
    return reason;
  }

}
