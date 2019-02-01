package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CsvEncoderServerSide {
  FileOutputStream outputStream;
  BufferedOutputStream buffer;
  static final String ENCODING = "UTF-8";
  final char separator;

  /**
   * Initialize the class to write into a file.
   * 
   * @param filename the file name
   * @param separator the separator for the CSV fields.
   */
  public CsvEncoderServerSide(String filename, char separator) {
    try {
      outputStream = new FileOutputStream(filename);
      buffer = new BufferedOutputStream(outputStream);
      buffer.write(239);// BOM for UTF-8
      buffer.write(187);// BOM for UTF-8
      buffer.write(191);// BOM for UTF-8
      this.separator = separator;
    } catch (IOException ex) {
      throw new FatalException("The file stream cannot be opened");
    }
  }

  /**
   * Write a string to the current position into the file. Does not return to line.
   * 
   * @param words the string to put into the file.
   */
  public void write(String[] words) {
    try {
      for (String word : words) {
        word += separator;
        byte[] textBytes = word.getBytes(ENCODING);
        buffer.write(textBytes);
      }
    } catch (IOException ex) {
      try {
        buffer.close();
      } catch (IOException ex2) {
        throw new FatalException("Closing the stream failed");
      }
      throw new FatalException("The writing on the stream failed");
    }
  }

  /**
   * Write a string to the current position into the file and end the line.
   * 
   * @param line the string to put into the file.
   */
  public void writeLine(String[] line) {
    try {
      write(line);
      buffer.write('\n');
    } catch (IOException ex) {
      try {
        buffer.close();
      } catch (IOException ex2) {
        throw new FatalException("Closing the stream failed");
      }
      throw new FatalException("The writing on the stream failed");
    }
  }

  /**
   * Close the opened stream.
   */
  public void close() {
    try {
      buffer.close();
    } catch (IOException ex) {
      throw new FatalException("Closing the stream failed");
    }
  }
}
