package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.persistence.DalServices;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The legacy thread-bound connection handling itself ({@code DalServicesImpl}). This class goes
 * away with the move to Spring-managed transactions; unlike the DAO tests it talks to
 * {@link DalServices} directly and must then be deleted, not ported.
 */
class DalServicesImplIT extends AbstractDaoIT {

  private static final String SCRATCH = "student_exchange_tools.dal_services_it_scratch";

  @Autowired
  private DalServices dalServices;

  @Autowired
  private DalBackendServices dalBackendServices;

  private int backendPid() {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement("SELECT pg_backend_pid()");
        ResultSet rs = stmt.executeQuery()) {
      rs.next();
      return rs.getInt(1);
    } catch (SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private boolean autoCommit() {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement("SELECT 1")) {
      return stmt.getConnection().getAutoCommit();
    } catch (SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private long scratchRows() {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(
        "SELECT count(*) FROM " + SCRATCH); ResultSet rs = stmt.executeQuery()) {
      rs.next();
      return rs.getLong(1);
    } catch (SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void update(String sql) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(sql)) {
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Test
  void nestedOpenConnectionCallsShareOneConnectionUntilTheOutermostClose() {
    dalServices.openConnection();
    try {
      int outer = backendPid();
      dalServices.openConnection();
      int inner = backendPid();
      dalServices.closeConnection();
      // still open: the inner close only decremented the counter
      assertThat(backendPid()).isEqualTo(outer).isEqualTo(inner);
    } finally {
      dalServices.closeConnection();
    }
  }

  @Test
  void startTransactionDisablesAutoCommitAndRollbackRestoresIt() {
    runInTransaction(() -> assertThat(autoCommit()).isFalse());
    dalServices.openConnection();
    try {
      assertThat(autoCommit()).isTrue();
    } finally {
      dalServices.closeConnection();
    }
  }

  @Test
  void rollbackDiscardsTheWorkAndCommitPublishesIt() {
    dalServices.openConnection();
    try {
      update("CREATE TABLE IF NOT EXISTS " + SCRATCH + " (id INTEGER)");
      update("TRUNCATE " + SCRATCH);

      dalServices.startTransaction();
      update("INSERT INTO " + SCRATCH + " VALUES (1)");
      dalServices.rollback();
      assertThat(autoCommit()).isTrue();
      assertThat(scratchRows()).isZero();

      dalServices.startTransaction();
      update("INSERT INTO " + SCRATCH + " VALUES (2)");
      dalServices.commit();
      assertThat(autoCommit()).isTrue();
    } finally {
      dalServices.closeConnection();
    }

    // visible from another connection once committed
    dalServices.openConnection();
    try {
      assertThat(scratchRows()).isEqualTo(1);
    } finally {
      update("DROP TABLE " + SCRATCH);
      dalServices.closeConnection();
    }
  }
}
