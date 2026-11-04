package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Denial reasons: init.sql inserts reason 1; no fixture needed. Denial reasons have no version
 * column, hence no optimistic locking, but an update of an unknown id fails like in the other DAOs.
 */
class DenialReasonRepositoryIT extends AbstractRepositoryIT {

  private static final String INITIAL_REASON = "L'étudiant part en mobilité pour ce semestre.";

  @Autowired private DenialReasonRepository denialReasonRepository;

  private DenialReason reason(String text) {
    DenialReason reason = new DenialReason();
    reason.setReason(text);
    return reason;
  }

  @Test
  void findAllReturnsTheReasonsOfInitSql() {
    List<DenialReason> reasons = inTransaction(() -> denialReasonRepository.findAll());

    assertThat(reasons)
        .extracting(DenialReason::getId, DenialReason::getReason)
        .containsExactly(tuple(1, INITIAL_REASON));
  }

  @Test
  void findByIdMapsIdAndReason() {
    DenialReason reason = inTransaction(() -> denialReasonRepository.findById(1).orElse(null));

    assertThat(reason.getId()).isEqualTo(1);
    assertThat(reason.getReason()).isEqualTo(INITIAL_REASON);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, -1})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> denialReasonRepository.findById(id))).isEmpty();
  }

  @Test
  void createReturnsTheGeneratedIdAndTheReasonIsListed() {
    runInTransaction(() -> {
      DenialReason created = denialReasonRepository.create(reason("Places insuffisantes"));

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(denialReasonRepository.findById(created.getId()).orElseThrow().getReason())
          .isEqualTo("Places insuffisantes");
      assertThat(denialReasonRepository.findAll())
          .extracting(DenialReason::getId)
          .containsExactlyInAnyOrder(1, created.getId());
    });
  }

  @Test
  void createRejectsAReasonLongerThanTheColumn() {
    runInTransaction(() ->
        assertThatThrownBy(() -> denialReasonRepository.create(reason("x".repeat(301))))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void updateReplacesTheReasonText() {
    runInTransaction(() -> {
      DenialReason reason = denialReasonRepository.findById(1).orElse(null);
      reason.setReason("Nouveau motif");

      assertThat(denialReasonRepository.update(reason)).isSameAs(reason);
      assertThat(denialReasonRepository.findById(1).orElseThrow().getReason())
          .isEqualTo("Nouveau motif");
    });
  }

  @Test
  void updateOfAnUnknownIdFailsAndWritesNothing() {
    runInTransaction(() -> {
      DenialReason ghost = reason("ghost");
      ghost.setId(31337);

      assertThatThrownBy(() -> denialReasonRepository.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class)
          .hasMessageContaining("31337");
      assertThat(denialReasonRepository.findAll())
          .extracting(DenialReason::getReason)
          .doesNotContain("ghost")
          .hasSize(1);
    });
  }
}
