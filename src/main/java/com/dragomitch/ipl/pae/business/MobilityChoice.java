package com.dragomitch.ipl.pae.business;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;

public interface MobilityChoice extends MobilityChoiceDto {
  int MAX_ORDER_CHOICE = 3;
  int MAX_TERM_CHOICE = 2;
  String MOBILITY_TYPE_SMS = "SMS";
  String MOBILITY_TYPE_SMP = "SMP";

  void checkDataIntegrity();

}
