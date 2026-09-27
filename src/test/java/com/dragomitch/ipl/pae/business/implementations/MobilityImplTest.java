package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.ALL_DEPARTURE_DOCUMENTS_NOT_FILLED_IN_503;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.ALL_DOCUMENTS_NOT_FILLED_IN_507;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.ALL_RETURN_DOCUMENTS_NOT_FILLED_IN_504;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.EXISTENCE_VIOLATION_DOCUMENT_505;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_CANCELED_MOBILITY_STATE_501;
import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_CLOSED_MOBILITY_STATE_502;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.Mobility;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Document and state rules of a mobility. */
class MobilityImplTest {

  private final EntityFactoryImpl factory = new EntityFactoryImpl();

  /**
   * Builds a mobility from a compact description: one token per document, "D" or "R" for the
   * category, followed by "+" when it is filled in (e.g. "D+ D R").
   */
  private Mobility mobility(String documents) {
    Mobility mobility = (Mobility) factory.build(Mobility.class);
    List<DocumentDto> list = new ArrayList<>();
    int id = 1;
    for (String token : documents.trim().isEmpty() ? new String[0] : documents.split(" ")) {
      DocumentDto document = (DocumentDto) factory.build(DocumentDto.class);
      document.setId(id++);
      document.setCategory(token.charAt(0));
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
      "D+ D+,       true,  true,  true"})
  void documentCompleteness(String documents, boolean all, boolean departure, boolean ret) {
    Mobility mobility = mobility(documents);

    assertThat(mobility.allDocumentsFilledIn()).isEqualTo(all);
    assertThat(mobility.allDepartureDocumentsFilledIn()).isEqualTo(departure);
    assertThat(mobility.allReturnDocumentsFilledIn()).isEqualTo(ret);
    assertOutcome(mobility::checkAllDocumentsFilledIn, all, ALL_DOCUMENTS_NOT_FILLED_IN_507);
    assertOutcome(mobility::checkAllDepartureDocumentsFilledIn, departure,
        ALL_DEPARTURE_DOCUMENTS_NOT_FILLED_IN_503);
    assertOutcome(mobility::checkAllReturnDocumentsFilledIn, ret,
        ALL_RETURN_DOCUMENTS_NOT_FILLED_IN_504);
  }

  private static void assertOutcome(ThrowingCallable check, boolean passes, int errorCode) {
    if (passes) {
      assertThatCode(check).doesNotThrowAnyException();
    } else {
      assertThat(Violations.errorCodeOf(check)).isEqualTo(errorCode);
    }
  }

  @Test
  void documentChecksNeedTheDocumentsToBeLoaded() {
    Mobility mobility = (Mobility) factory.build(Mobility.class);

    assertThatThrownBy(mobility::allDocumentsFilledIn).isInstanceOf(FatalException.class)
        .hasMessage(FatalException.LAZY_LOADING_ERROR_MSG);
    assertThatThrownBy(mobility::allDepartureDocumentsFilledIn)
        .isInstanceOf(FatalException.class);
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
        .isEqualTo(EXISTENCE_VIOLATION_DOCUMENT_505);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void fillInDocumentRejectsANonPositiveId(int id) {
    Mobility mobility = mobility("D");

    assertThatThrownBy(() -> mobility.fillInDocument(id))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "state {0}: notCancelled={1} notClosed={2}")
  @CsvSource({
      "Créée,          true,  true",
      "En préparation, true,  true",
      "A payer,        true,  true",
      "En cours,       true,  true",
      "Solde à payer,  true,  true",
      "Terminée,       true,  false",
      "Annulée,        false, true"})
  void stateChecks(String state, boolean notCancelled, boolean notClosed) {
    Mobility mobility = (Mobility) factory.build(Mobility.class);
    mobility.setState(state);

    assertOutcome(mobility::checkNotCancelled, notCancelled, INVALID_CANCELED_MOBILITY_STATE_501);
    assertOutcome(mobility::checkNotClosed, notClosed, INVALID_CLOSED_MOBILITY_STATE_502);
    int combinedError = !notCancelled ? INVALID_CANCELED_MOBILITY_STATE_501
        : INVALID_CLOSED_MOBILITY_STATE_502;
    assertOutcome(mobility::checkNotCancelledAndNotClosed, notCancelled && notClosed,
        combinedError);
  }

  @Test
  void theStateConstantsMatchTheValuesStoredInTheDatabase() {
    assertThat(List.of(MobilityDto.STATE_CREATED, MobilityDto.STATE_IN_PREPARATION,
        MobilityDto.STATE_TO_BE_PAID, MobilityDto.STATE_IN_PROGRESS,
        MobilityDto.STATE_BALANCE_TO_BE_PAID, MobilityDto.STATE_CLOSED,
        MobilityDto.STATE_CANCELLED)).containsExactly("Créée", "En préparation", "A payer",
            "En cours", "Solde à payer", "Terminée", "Annulée");
  }
}
