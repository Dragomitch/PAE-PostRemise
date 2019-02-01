package com.dragomitch.ipl.pae.business;

public interface EntityFactory {

  /**
   * Builds an entity of class {@code cl} and returns it.
   * 
   * @param cl the entity class
   * @return an instantiated entity
   */
  Object build(Class<?> cl);

}
