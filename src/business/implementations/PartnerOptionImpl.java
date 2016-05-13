package business.implementations;

import business.PartnerOption;
import persistence.DaoClass;
import persistence.PartnerOptionDao;

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
