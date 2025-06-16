package com.dragomitch.ipl.pae.business.implementations;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import com.dragomitch.ipl.pae.business.PartnerOption;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

@DaoClass(PartnerOptionDao.class)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
