package com.dragomitch.ipl.pae.domain.model;

import static com.dragomitch.ipl.pae.domain.exceptions.ErrorCode.DEPARTURE_DOCUMENTS_INCOMPLETE;
import static com.dragomitch.ipl.pae.domain.exceptions.ErrorCode.DOCUMENTS_INCOMPLETE;
import static com.dragomitch.ipl.pae.domain.exceptions.ErrorCode.MOBILITY_CANCELLED;
import static com.dragomitch.ipl.pae.domain.exceptions.ErrorCode.MOBILITY_CLOSED;
import static com.dragomitch.ipl.pae.domain.exceptions.ErrorCode.RETURN_DOCUMENTS_INCOMPLETE;
import static com.dragomitch.ipl.pae.domain.exceptions.ErrorCode.UNKNOWN_DOCUMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.FatalException;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Document and state rules of a mobility. */
class MobilityImplTest {

  /**
   * Builds a mobility from a compact description: one token per document, "D" or "R" for the
   * category, followed by "+" when it is filled in (e.g. "D+ D R").
   */
  private Mobility mobility(String documents) {
    Mobility mobility = new Mobility();
    List<Document> list = new ArrayList<>();
    int id = 1;
    for (String token : documents.trim().isEmpty() ? new String[0] : documents.split(" ")) {
      Document document = new Document();
      document.setId(id++);
      document.setCategory(DocumentCategory.fromCode(token.charAt(0)));
      document.setFilledIn(token.endsWith("+"));
      list.add(document);
    }
    mobility.setDocuments(list);
    return mobility;
  }

  @ParameterizedTest(name = "[{0}] all={1} departure={2} return={3}")
  @CsvSource({
    "'',          true,  true,  true",
    "D+ R+,       true,  true,  true",
    "D+ D R+,     false, false, true",
    "D+ R,        false, true,  false",
    "D R,         false, false, false",
    "D+ D+,       true,  true,  true"
  })
  void documentCompleteness(String documents, boolean all, boolean departure, boolean ret) {
    Mobility mobility = mobility(documents);

    assertThat(mobility.allDocumentsFilledIn()).isEqualTo(all);
    assertThat(mobility.allDepartureDocumentsFilledIn()).isEqualTo(departure);
    assertThat(mobility.allReturnDocumentsFilledIn()).isEqualTo(ret);
    assertOutcome(mobility::checkAllDocumentsFilledIn, all, DOCUMENTS_INCOMPLETE);
    assertOutcome(
        mobility::checkAllDepartureDocumentsFilledIn, departure, DEPARTURE_DOCUMENTS_INCOMPLETE);
    assertOutcome(mobility::checkAllReturnDocumentsFilledIn, ret, RETURN_DOCUMENTS_INCOMPLETE);
  }

  private static void assertOutcome(ThrowingCallable check, boolean passes, ErrorCode errorCode) {
    if (passes) {
      assertThatCode(check).doesNotThrowAnyException();
    } else {
      assertThat(Violations.errorCodeOf(check)).isEqualTo(errorCode);
    }
  }

  @Test
  void documentChecksNeedTheDocumentsToBeLoaded() {
    Mobility mobility = new Mobility();

    assertThatThrownBy(mobility::allDocumentsFilledIn)
        .isInstanceOf(FatalException.class)
        .hasMessage(FatalException.LAZY_LOADING_ERROR_MSG);
    assertThatThrownBy(mobility::allDepartureDocumentsFilledIn).isInstanceOf(FatalException.class);
    assertThatThrownBy(mobility::allReturnDocumentsFilledIn).isInstanceOf(FatalException.class);
    assertThatThrownBy(() -> mobility.fillInDocument(1)).isInstanceOf(FatalException.class);
  }

  @Test
  void fillInDocumentMarksAnUnfilledDocumentAndReportsTheChange() {
    Mobility mobility = mobility("D D+");

    assertThat(mobility.fillInDocument(1)).isTrue();
    assertThat(mobility.getDocuments().get(0).isFilledIn()).isTrue();
    assertThat(mobility.fillInDocument(1)).as("already filled in").isFalse();
    assertThat(mobility.fillInDocument(2)).isFalse();
  }

  @Test
  void fillInDocumentOfAnUnknownDocumentFails() {
    Mobility mobility = mobility("D");

    assertThat(Violations.errorCodeOf(() -> mobility.fillInDocument(9)))
        .isEqualTo(UNKNOWN_DOCUMENT);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void aNonPositiveIdIsAnUnknownDocument(int id) {
    // the use case rejects it before (@Positive document of MobilityService.confirmDocument)
    Mobility mobility = mobility("D");

    assertThat(Violations.errorCodeOf(() -> mobility.fillInDocument(id)))
        .isEqualTo(UNKNOWN_DOCUMENT);
  }

  @ParameterizedTest(name = "state {0}: notCancelled={1} notClosed={2}")
  @CsvSource({
    "CREATED,        true,  true",
    "IN_PREPARATION, true,  true",
    "TO_BE_PAID,     true,  true",
    "IN_PROGRESS,    true,  true",
    "BALANCE_TO_BE_PAID, true,  true",
    "CLOSED,         true,  false",
    "CANCELLED,      false, true"
  })
  void stateChecks(MobilityState state, boolean notCancelled, boolean notClosed) {
    Mobility mobility = new Mobility();
    mobility.setState(state);

    assertOutcome(mobility::checkNotCancelled, notCancelled, MOBILITY_CANCELLED);
    assertOutcome(mobility::checkNotClosed, notClosed, MOBILITY_CLOSED);
    ErrorCode combinedError = !notCancelled ? MOBILITY_CANCELLED : MOBILITY_CLOSED;
    assertOutcome(
        mobility::checkNotCancelledAndNotClosed, notCancelled && notClosed, combinedError);
  }
}
