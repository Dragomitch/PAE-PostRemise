package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.context.ContextManager;

class EntityFactoryImpl implements EntityFactory {

  /**
   * Returns an instance of the Class cl.
   * 
   * @param cl the Class that the caller needs an instance of
   * @return an instance of the given class
   */
  @Override
  public Object build(Class<?> cl) {
    try {
      String implName = ContextManager.getProperty(cl.getName());
      Class<?> implClass = Class.forName(implName);
      return implClass.newInstance();
    } catch (InstantiationException ex) {
      // TODO Gérer les exceptions
      ex.printStackTrace();
    } catch (IllegalAccessException ex) {
      // TODO Gérer les exceptions
      ex.printStackTrace();
    } catch (ClassNotFoundException ex) {
      // TODO Gérer les exceptions
      ex.printStackTrace();
    }
    return null;
  }

}
