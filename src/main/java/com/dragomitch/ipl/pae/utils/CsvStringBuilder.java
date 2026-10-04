package com.dragomitch.ipl.pae.utils;

public class CsvStringBuilder {
  static final String UTF8_BOM = "\uFEFF";
  private final char separator;
  private final StringBuilder csvContent;

  /**
   * Initialize the parameters create a CSV string.
   * 
   * @param separator the separator for the CSV fields.
   */
  public CsvStringBuilder(char separator) {
    this.separator = separator;
    csvContent = new StringBuilder(UTF8_BOM);
  }

  /**
   * Write all the strings to the current position into the CSV. Does not return to line.
   * 
   * @param words the string to put into the file.
   */
  public void write(String[] words) {
    for (String word : words) {
      csvContent.append(word);
      csvContent.append(separator);
    }
  }

  /**
   * Write a string to the current position into the CSV. Does not return to line.
   * 
   * @param word the string to put into the CSV.
   */
  public void write(String word) {
    csvContent.append(word);
    csvContent.append(separator);
  }

  /**
   * Write all the strings to the current position into the CSV. Return to line after last string.
   * 
   * @param line the strings to put into the CSV.
   */
  public void writeLine(String[] line) {
    write(line);
    csvContent.append('\n');
  }

  /**
   * Close the current CSV file.
   * 
   * @return the CSV generated.
   */
  public String close() {
    return csvContent.toString();
  }
}
