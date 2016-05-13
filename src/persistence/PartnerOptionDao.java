package persistence;

import business.dto.PartnerDto;
import business.dto.PartnerOptionDto;

import java.util.List;

public interface PartnerOptionDao {

  String TABLE_NAME = "partner_options";
  String COLUMN_OPTION_CODE = "option_code";
  String COLUMN_PARTNER_ID = "partner_id";
  String COLUMN_DEPARTEMENT = "departement";

  /**
   * Inserts a partner option into the database.
   * 
   * @param partnerOption the partnerOption to be inserted.
   * @param partnerId the id of the partner owning the option.
   * @return the partnerOption that has been just created.
   */
  PartnerOptionDto create(PartnerOptionDto partnerOption, int partnerId);

  /**
   * Queries the database for all partners corresponding to an option.
   * 
   * @param optionCode the code of the option for which we want to retrieve partners from the
   *        Database.
   * @return All partners concerned by the option.
   */
  List<PartnerDto> findAllPartnersByOption(String optionCode);

  /**
   * Queries the database for all options corresponding to a partner.
   * 
   * @param partnerId the id of the partner for which we want to retrieve options from the Database.
   * @return All options concerned by the partner.
   */
  List<PartnerOptionDto> findAllOptionsByPartner(int partnerId);

}
