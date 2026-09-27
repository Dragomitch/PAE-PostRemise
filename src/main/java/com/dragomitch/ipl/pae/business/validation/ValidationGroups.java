package com.dragomitch.ipl.pae.business.validation;

import jakarta.validation.groups.Default;

/**
 * Bean Validation groups of the DTO constraints.
 *
 * <ul>
 * <li>{@link Default}: the rules of an entity sent in full (a user edited, an address, a
 * nominated student...).</li>
 * <li>{@link OnCreate}: the extra rules of a creation (a password at sign-up, at least one option
 * for a new partner), validated together with {@code Default}
 * ({@code @Validated({Default.class, OnCreate.class})} on the use case).</li>
 * <li>{@link Reference}: an entity only referenced by another one (the {@code option} of a user,
 * the {@code country} of an address, the {@code user} of a mobility choice...): only its
 * identifier is checked. The referencing getter converts the group with
 * {@code @Valid @ConvertGroup(to = Reference.class)}.</li>
 * </ul>
 */
public final class ValidationGroups {

  private ValidationGroups() {
  }

  /** Extra rules of a creation, validated on top of the {@link Default} ones. */
  public interface OnCreate {
  }

  /** Rules of an entity referenced by another one: its identifier only. */
  public interface Reference {
  }
}
