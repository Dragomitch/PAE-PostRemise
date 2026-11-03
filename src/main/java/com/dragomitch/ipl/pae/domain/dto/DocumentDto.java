package com.dragomitch.ipl.pae.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public interface DocumentDto extends Entity {

  public static final char DEPARTURE_DOCUMENT = 'D';
  public static final char RETURN_DOCUMENT = 'R';

  String getName();

  void setName(String name);

  @Schema(description = "D (departure document) or R (return document).")
  char getCategory();

  void setCategory(char category);

  boolean isFilledIn();

  void setFilledIn(boolean isFilledIn);

  // TODO Supprimer cela car pas nécessaire
  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

}
