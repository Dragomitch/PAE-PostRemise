package persistence.mocks;

import persistence.DalServices;

public class MockDalServices implements DalServices {

  @Override
  public void startTransaction() {}

  @Override
  public void commit() {}

  @Override
  public void rollback() {}

  @Override
  public void openConnection() {}

  @Override
  public void closeConnection() {}

}
