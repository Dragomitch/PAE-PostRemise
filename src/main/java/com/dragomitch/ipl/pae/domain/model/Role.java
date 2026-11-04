package com.dragomitch.ipl.pae.domain.model;

import java.util.Arrays;

/**
 * The role of a user account: the first account signed up is a professor, the next ones are
 * students (a professor can promote them). The label is the value stored in {@code users.role},
 * sent in the JSON of a user and carried by the {@code role} claim of the session.
 */
public enum Role {
  PROFESSOR("Professor"),
  STUDENT("Student");

  private final String label;

  Role(String label) {
    this.label = label;
  }

  /** The stored and serialized value: {@code Professor} or {@code Student}. */
  public String label() {
    return label;
  }

  /**
   * The role of a label.
   *
   * @param label {@code Professor} or {@code Student} (case-sensitive)
   * @return the role
   * @throws IllegalArgumentException if no role has that label
   */
  public static Role fromLabel(String label) {
    return Arrays.stream(values())
        .filter(role -> role.label.equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + label));
  }
}
