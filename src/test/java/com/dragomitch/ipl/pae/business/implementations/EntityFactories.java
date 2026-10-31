package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;

/**
 * The real {@link EntityFactory} outside a Spring context, for the Mockito-based unit tests of the
 * use cases: they work on the real business objects (whose rules they exercise) while the DAOs are
 * Mockito mocks.
 */
public final class EntityFactories {

  private EntityFactories() {
  }

  /** A new factory of the real business objects. */
  public static EntityFactory create() {
    return new EntityFactoryImpl();
  }
}
