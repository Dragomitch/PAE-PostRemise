package ucc;

import business.dto.Entity;

public interface UnitOfWork {

  /**
   * Start a new business transaction.
   */
  void startTransaction();

  /**
   * Notify the unit of work about an update operation to be performed.
   * 
   * @param entity the entity to update
   * @return the updated entity
   */
  Entity update(Entity entity);

  /**
   * End a business transaction. If the unit of work performs all the operations successfully, it
   * commits all the changes to the database. Otherwise, it cancels the changes and restore them to
   * their previous state.
   */
  void commit();

  /**
   * End a business transaction, cancels the upcoming changes to be made and restore modifications
   * made in database during the transaction to their previous state.
   */
  void rollback();


}
