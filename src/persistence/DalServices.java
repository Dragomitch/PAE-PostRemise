package persistence;

public interface DalServices {

  /**
   * Start a transaction.
   */
  void startTransaction();

  /**
   * Commit the operations that have been made during a transaction.
   */
  void commit();

  /**
   * Bring back the database to the state it uses to have at the start of the transaction.
   */
  void rollback();

  /**
   * Open a connection if not already openened.
   */
  void openConnection();

  /**
   * Close a connection if not already closed.
   */
  void closeConnection();
}
