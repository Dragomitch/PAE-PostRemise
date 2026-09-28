package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Denial reasons: init.sql inserts reason 1; no fixture needed. Denial reasons have no version
 * column, hence no optimistic locking, but an update of an unknown id fails like in the other DAOs.
 */
class DenialReasonDaoIT extends AbstractDaoIT {

  private static final String INITIAL_REASON = "L'étudiant part en mobilité pour ce semestre.";

  @Autowired
  private DenialReasonDao denialReasonDao;

  private DenialReasonDto reason(String text) {
    DenialReasonDto reason = build(DenialReasonDto.class);
    reason.setReason(text);
    return reason;
  }

  @Test
  void findAllReturnsTheReasonsOfInitSql() {
    List<DenialReasonDto> reasons = inTransaction(() -> denialReasonDao.findAll());

    assertThat(reasons).extracting(DenialReasonDto::getId, DenialReasonDto::getReason)
        .containsExactly(tuple(1, INITIAL_REASON));
  }

  @Test
  void findByIdMapsIdAndReason() {
    DenialReasonDto reason = inTransaction(() -> denialReasonDao.findById(1));

    assertThat(reason.getId()).isEqualTo(1);
    assertThat(reason.getReason()).isEqualTo(INITIAL_REASON);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, -1})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> denialReasonDao.findById(id))).isNull();
  }

  @Test
  void createReturnsTheGeneratedIdAndTheReasonIsListed() {
    runInTransaction(() -> {
      DenialReasonDto created = denialReasonDao.create(reason("Places insuffisantes"));

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(denialReasonDao.findById(created.getId()).getReason())
          .isEqualTo("Places insuffisantes");
      assertThat(denialReasonDao.findAll()).extracting(DenialReasonDto::getId)
          .containsExactlyInAnyOrder(1, created.getId());
    });
  }

  @Test
  void createRejectsAReasonLongerThanTheColumn() {
    runInTransaction(() -> assertThatThrownBy(() -> denialReasonDao.create(reason("x".repeat(301))))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void updateReplacesTheReasonText() {
    runInTransaction(() -> {
      DenialReasonDto reason = denialReasonDao.findById(1);
      reason.setReason("Nouveau motif");

      assertThat(denialReasonDao.update(reason)).isSameAs(reason);
      assertThat(denialReasonDao.findById(1).getReason()).isEqualTo("Nouveau motif");
    });
  }

  @Test
  void updateOfAnUnknownIdFailsAndWritesNothing() {
    runInTransaction(() -> {
      DenialReasonDto ghost = reason("ghost");
      ghost.setId(31337);

      assertThatThrownBy(() -> denialReasonDao.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class).hasMessageContaining("31337");
      assertThat(denialReasonDao.findAll()).extracting(DenialReasonDto::getReason)
          .doesNotContain("ghost").hasSize(1);
    });
  }
}
