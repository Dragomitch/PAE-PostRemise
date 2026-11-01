package com.dragomitch.ipl.pae.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.Objects;
import java.util.function.Supplier;

/** The version contract of the updates of the driven ports. */
final class Versions {

  private Versions() {
  }

  /**
   * Writes the pending changes of a versioned entity and increments its version, even when nothing
   * changed.
   *
   * <p>JPA only increments the version of a changed entity. The ports increment it on every update,
   * as the JDBC DAOs did: the use cases rely on it, for example a partner edit, which takes the
   * stored partner version and so depends on its address update to detect a stale form.
   *
   * @param entityManager the persistence context of the entity
   * @param entity the managed entity, whose version was checked against the DTO's
   * @param version the version of the entity
   * @param readVersion the version the entity had when it was read
   */
  static void flushIncrementingVersion(EntityManager entityManager, Object entity,
      Supplier<Integer> version, int readVersion) {
    entityManager.flush();
    if (Objects.equals(version.get(), readVersion)) {
      entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
    }
  }
}
