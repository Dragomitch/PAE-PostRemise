package main.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogConsole extends ConsoleHandler {

  @Override
  public void publish(LogRecord record) {
    String formattedRecord = getFormatter().format(record);
    if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
      System.err.println(formattedRecord);
    } else {
      System.out.println(formattedRecord);
    }
  }

}
