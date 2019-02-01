package com.dragomitch.ipl.pae.business.dto;

public interface ProgrammeDto extends Entity {

  String getProgrammeName();

  void setProgrammeName(String programmeName);

  String getExternalSoftName();

  void setExternalSoftName(String softwareName);

}
