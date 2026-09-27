package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.persistence.DalServices;

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
