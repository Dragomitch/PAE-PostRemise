package com.dragomitch.ipl.pae.domain.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The life cycle of a mobility, from the confirmation of its choice to its last payment. The French
 * label is the value stored in {@code mobilities.state} and sent in the JSON, as the web UIs show
 * it.
 *
 * <pre>
 * Créée ──► En préparation ──► A payer ──► En cours ──► Solde à payer ──► Terminée
 *   └──────────────────────────►┘   └──────────────────►┘
 * every state but Terminée ──► Annulée
 * </pre>
 *
 * A mobility starts {@link #CREATED}, or {@link #TO_BE_PAID} when its programme has no departure
 * document. Filling in documents moves it to {@link #IN_PREPARATION}, then to {@link #TO_BE_PAID}
 * (every departure document filled in); the first payment to {@link #IN_PROGRESS}, or straight to
 * {@link #BALANCE_TO_BE_PAID} when its return documents are already filled in; the return documents
 * to {@link #BALANCE_TO_BE_PAID}; the second payment to {@link #CLOSED}.
 */
public enum MobilityState {
  CREATED("Créée"),
  IN_PREPARATION("En préparation"),
  TO_BE_PAID("A payer"),
  IN_PROGRESS("En cours"),
  BALANCE_TO_BE_PAID("Solde à payer"),
  CLOSED("Terminée"),
  CANCELLED("Annulée");

  private final String label;

  MobilityState(String label) {
    this.label = label;
  }

  /** The stored and serialized value, e.g. {@code En préparation}. */
  public String label() {
    return label;
  }

  /** The states this one may move to; none from {@link #CLOSED} and {@link #CANCELLED}. */
  public Set<MobilityState> nextStates() {
    return switch (this) {
      case CREATED -> EnumSet.of(IN_PREPARATION, TO_BE_PAID, CANCELLED);
      case IN_PREPARATION -> EnumSet.of(TO_BE_PAID, CANCELLED);
      case TO_BE_PAID -> EnumSet.of(IN_PROGRESS, BALANCE_TO_BE_PAID, CANCELLED);
      case IN_PROGRESS -> EnumSet.of(BALANCE_TO_BE_PAID, CANCELLED);
      case BALANCE_TO_BE_PAID -> EnumSet.of(CLOSED, CANCELLED);
      case CLOSED, CANCELLED -> EnumSet.noneOf(MobilityState.class);
    };
  }

  /**
   * Tells whether a mobility in this state may move to another one.
   *
   * @param next the next state
   * @return true if the transition is allowed
   */
  public boolean canMoveTo(MobilityState next) {
    return nextStates().contains(next);
  }

  /**
   * The state of a label.
   *
   * @param label a label, e.g. {@code Créée} (case-sensitive)
   * @return the state
   * @throws IllegalArgumentException if no state has that label
   */
  public static MobilityState fromLabel(String label) {
    return Arrays.stream(values())
        .filter(state -> state.label.equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown mobility state: " + label));
  }
}
