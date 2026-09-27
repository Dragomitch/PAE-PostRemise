package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.constraints.Positive;

/**
 * An entity with a generated identifier and a version (optimistic locking).
 *
 * <p>The Bean Validation constraints of the DTOs are declared on the getters of these interfaces
 * (Hibernate Validator applies them to every implementation) so that the rules are documented
 * with the contract they apply to. See {@code ValidationGroups} for the groups.
 */
public interface Entity {

  /** The identifier; an entity referenced by another one must have a positive one. */
  @Positive(groups = Reference.class)
  public int getId();

  public void setId(int id);

  public int getVersion();

  public void setVersion(int id);

}
