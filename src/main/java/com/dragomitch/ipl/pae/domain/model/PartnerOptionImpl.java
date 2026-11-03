package com.dragomitch.ipl.pae.domain.model;

public class PartnerOptionImpl extends OptionImpl implements PartnerOption {

  private static final long serialVersionUID = 1L;

  private String departement;

  @Override
  public String getDepartement() {
    return departement;
  }

  @Override
  public void setDepartement(String departement) {
    this.departement = departement;
  }
}
