package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import java.util.List;

public interface PartnerOptionRepository {

  /**
   * Inserts a partner option into the database.
   *
   * @param partnerOption the partnerOption to be inserted.
   * @param partnerId the id of the partner owning the option.
   * @return the partnerOption that has been just created.
   */
  PartnerOption create(PartnerOption partnerOption, int partnerId);

  /**
   * Queries the database for all partners corresponding to an option.
   *
   * @param optionCode the code of the option for which we want to retrieve partners from the
   *     Database.
   * @return All partners concerned by the option.
   */
  List<Partner> findAllPartnersByOption(String optionCode);

  /**
   * Queries the database for all options corresponding to a partner.
   *
   * @param partnerId the id of the partner for which we want to retrieve options from the Database.
   * @return All options concerned by the partner.
   */
  List<PartnerOption> findAllOptionsByPartner(int partnerId);
}
