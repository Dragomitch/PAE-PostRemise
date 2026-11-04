package com.dragomitch.ipl.pae.domain.model;

/** An option offered by a partner, in one of its departments. */
public class PartnerOption extends Option {

  private String departement;

  public String getDepartement() {
    return departement;
  }

  public void setDepartement(String departement) {
    this.departement = departement;
  }
}
