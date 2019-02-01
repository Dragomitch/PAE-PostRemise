package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.PartnerOption;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

@DaoClass(PartnerOptionDao.class)
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
