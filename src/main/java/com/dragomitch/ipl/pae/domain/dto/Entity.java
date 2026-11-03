package com.dragomitch.ipl.pae.domain.dto;

import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * An entity with a generated identifier and a version (optimistic locking).
 *
 * <p>The Bean Validation constraints of the DTOs are declared on the getters of these interfaces
 * (Hibernate Validator applies them to every implementation) so that the rules are documented with
 * the contract they apply to. See {@code ValidationGroups} for the groups.
 */
public interface Entity {

  /** The identifier; an entity referenced by another one must have a positive one. */
  @Positive(groups = Reference.class)
  public int getId();

  public void setId(int id);

  @Schema(
      description =
          "Optimistic locking: send back the version that was read; a stale one is "
              + "refused with a 409 CONCURRENT_MODIFICATION. Every update increments it.")
  public int getVersion();

  public void setVersion(int id);
}
