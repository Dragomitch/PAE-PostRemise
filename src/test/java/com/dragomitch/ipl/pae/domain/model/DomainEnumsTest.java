package com.dragomitch.ipl.pae.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The enums of the domain carry the values the database stores and the API exchanges: they must not
 * change, the legacy UI, the Angular app and the stored rows depend on them.
 */
class DomainEnumsTest {

  @Test
  void theRolesKeepTheirStoredLabels() {
    assertThat(Arrays.stream(Role.values()).map(Role::label))
        .containsExactly("Professor", "Student");
    assertThat(Role.fromLabel("Professor")).isEqualTo(Role.PROFESSOR);
    assertThat(Role.fromLabel("Student")).isEqualTo(Role.STUDENT);
    assertThatThrownBy(() -> Role.fromLabel("STUDENT"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Role.fromLabel(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void theMobilityStatesKeepTheirFrenchLabels() {
    assertThat(Arrays.stream(MobilityState.values()).map(MobilityState::label))
        .containsExactly(
            "Créée",
            "En préparation",
            "A payer",
            "En cours",
            "Solde à payer",
            "Terminée",
            "Annulée");
    for (MobilityState state : MobilityState.values()) {
      assertThat(MobilityState.fromLabel(state.label())).isEqualTo(state);
    }
    assertThatThrownBy(() -> MobilityState.fromLabel("CREATED"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "{0} -> {1}: {2}")
  @CsvSource({
    "CREATED, IN_PREPARATION, true",
    "CREATED, TO_BE_PAID, true",
    "CREATED, IN_PROGRESS, false",
    "IN_PREPARATION, TO_BE_PAID, true",
    "IN_PREPARATION, CREATED, false",
    "TO_BE_PAID, IN_PROGRESS, true",
    "TO_BE_PAID, BALANCE_TO_BE_PAID, true",
    "TO_BE_PAID, CLOSED, false",
    "IN_PROGRESS, BALANCE_TO_BE_PAID, true",
    "IN_PROGRESS, CLOSED, false",
    "BALANCE_TO_BE_PAID, CLOSED, true",
    "BALANCE_TO_BE_PAID, IN_PROGRESS, false",
    "CLOSED, CANCELLED, false",
    "CANCELLED, CREATED, false"
  })
  void theStateMachineAllowsTheDocumentedTransitionsOnly(
      MobilityState from, MobilityState to, boolean allowed) {
    assertThat(from.canMoveTo(to)).isEqualTo(allowed);
  }

  @ParameterizedTest
  @EnumSource(
      value = MobilityState.class,
      names = {"CLOSED", "CANCELLED"},
      mode = EnumSource.Mode.EXCLUDE)
  void anOpenMobilityCanBeCancelled(MobilityState state) {
    assertThat(state.canMoveTo(MobilityState.CANCELLED)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(
      value = MobilityState.class,
      names = {"CLOSED", "CANCELLED"})
  void aClosedOrCancelledMobilityIsFinal(MobilityState state) {
    assertThat(state.nextStates()).isEmpty();
  }

  @Test
  void theMobilityTypesAreThoseOfErasmus() {
    assertThat(Arrays.stream(MobilityType.values()).map(MobilityType::name))
        .containsExactly("SMS", "SMP");
  }

  @Test
  void theDocumentCategoriesKeepTheirCodes() {
    assertThat(DocumentCategory.DEPARTURE.code()).isEqualTo('D');
    assertThat(DocumentCategory.RETURN.code()).isEqualTo('R');
    assertThat(DocumentCategory.fromCode('D')).isEqualTo(DocumentCategory.DEPARTURE);
    assertThat(DocumentCategory.fromCode('R')).isEqualTo(DocumentCategory.RETURN);
    assertThatThrownBy(() -> DocumentCategory.fromCode('X'))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest(name = "{0} keeps a {1} document filled in={2}: {3}")
  @CsvSource({
    "D,  DEPARTURE, false, true",
    "D,  DEPARTURE, true,  true",
    "D,  RETURN,    true,  false",
    "DF, DEPARTURE, true,  true",
    "DF, DEPARTURE, false, false",
    "DF, RETURN,    true,  false",
    "R,  RETURN,    false, true",
    "R,  DEPARTURE, false, false",
    "RF, RETURN,    true,  true",
    "RF, RETURN,    false, false"
  })
  void theDocumentFiltersKeepTheirCategory(
      DocumentFilter filter, DocumentCategory category, boolean filledIn, boolean kept) {
    assertThat(filter.keeps(category, filledIn)).isEqualTo(kept);
  }

  @Test
  void theDocumentFiltersAreTheQueryValues() {
    assertThat(Arrays.stream(DocumentFilter.values()).map(DocumentFilter::name))
        .containsExactly("D", "DF", "R", "RF");
    assertThat(DocumentFilter.DF.category()).isEqualTo(DocumentCategory.DEPARTURE);
    assertThat(DocumentFilter.RF.filledInOnly()).isTrue();
    assertThat(DocumentFilter.R.filledInOnly()).isFalse();
  }

  @Test
  void theMobilityChoiceFiltersAreTheQueryValues() {
    assertThat(Arrays.stream(MobilityChoiceFilter.values()).map(MobilityChoiceFilter::value))
        .containsExactly("all", "active", "canceled", "rejected", "passed");
    for (MobilityChoiceFilter filter : MobilityChoiceFilter.values()) {
      assertThat(MobilityChoiceFilter.fromValue(filter.value())).isEqualTo(filter);
    }
    assertThatThrownBy(() -> MobilityChoiceFilter.fromValue("ALL"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void thePartnerFiltersAreTheQueryValues() {
    assertThat(Arrays.stream(PartnerFilter.values()).map(PartnerFilter::value))
        .containsExactly("all", "archived", "country");
    for (PartnerFilter filter : PartnerFilter.values()) {
      assertThat(PartnerFilter.fromValue(filter.value())).isEqualTo(filter);
    }
    assertThatThrownBy(() -> PartnerFilter.fromValue("name"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
