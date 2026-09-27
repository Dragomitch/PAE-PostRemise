package com.dragomitch.ipl.pae.business;

import java.util.Map;

public interface EntityFactory {

  /**
   * Builds an entity of class {@code cl} and returns it.
   * 
   * @param cl the entity class (a business or DTO interface)
   * @return an instantiated entity
   * @throws IllegalArgumentException if no implementation is bound to {@code cl}
   */
  Object build(Class<?> cl);

  /**
   * Returns every interface this factory can build, mapped to its implementation class. Used to
   * tell the JSON mapper how to instantiate interface-typed properties.
   *
   * @return an unmodifiable interface-to-implementation map
   */
  Map<Class<?>, Class<?>> getImplementations();

}
