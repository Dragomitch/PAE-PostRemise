package com.dragomitch.ipl.pae.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The CSV of the exports: UTF-8 byte order mark (for spreadsheets), every field followed by the
 * separator, lines ended by a line feed.
 */
class CsvStringBuilderTest {

  @ParameterizedTest
  @ValueSource(chars = {';', ',', '\t'})
  void fieldsAreFollowedByTheSeparatorAndLinesByALineFeed(char separator) {
    CsvStringBuilder csv = new CsvStringBuilder(separator);

    csv.writeLine(new String[] {"Nom", "Prénom"});
    csv.write(new String[] {"Martin", "Alice"});
    csv.write("");
    csv.write("Rempli");

    String s = String.valueOf(separator);
    assertThat(csv.close()).isEqualTo("﻿" + "Nom" + s + "Prénom" + s + "\n"
        + "Martin" + s + "Alice" + s + s + "Rempli" + s);
  }

  @ParameterizedTest
  @ValueSource(chars = {';', ','})
  void anEmptyCsvIsTheByteOrderMark(char separator) {
    assertThat(new CsvStringBuilder(separator).close()).isEqualTo("﻿");
  }
}
