package main.logging;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import main.ContextManager;

public class LogFilter implements Filter {

  @Override
  public boolean isLoggable(LogRecord record) {
    if (ContextManager.getEnvironment().equals(ContextManager.ENV_DEVELOPMENT)) {
      return true;
    }
    return record.getLevel().intValue() >= Level.WARNING.intValue();
  }

}
