package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;

import java.util.List;
import java.util.Map;

public interface PartnerUcc {

  /**
   * Create a partner.
   * 
   * @param partner : the partnerDto coming from the front-end
   * @param userRole : the role of the user who wants to create a new partner
   * @return the partnerDto that has just been created
   */
  PartnerDto create(PartnerDto partner, String userRole);

  /**
   * Find a partnerDto existing in the database.
   * 
   * @param id : the id of the partner we want to find
   * @return the partnerDto found in the database
   */
  PartnerDto showOne(int id);

  /**
   * Find all PartnerDao.
   * 
   * @param filter : the filter to apply
   * @param value : the value of the filter
   * @param userId : the id of the user who wants to edit a partner.
   * @param userRole : the role of the user who wants to edit a partner.
   * @return a list of all the partnerDto found in the database
   */
  Map<String, Object> showAll(String filter, String value, String userRole, int userId);

  /**
   * Edit a partner.
   * 
   * @param id : the id of the partnerDto we want to edit coming from the front-end.
   * @param partner : the partnerDto containing the new data.
   * @param userRole : the role of the user who wants to edit a partner.
   * @return the partnerDto that has just been edited.
   */
  PartnerDto edit(int id, PartnerDto partner, String userRole);

  PartnerDto restore(int id, String role);

  /**
   * Add an option to a partner.
   * 
   * @param id : the id of the partner for which we want to add an option.
   * @param partnerOption : the partnerOption of the we want to add.
   */
  void addOption(int id, PartnerOptionDto partnerOption);

  /**
   * Find all options for a partner.
   * 
   * @param partnerId : the id of the partnerDto for which we want to find options
   * @return a list of options corresponding to a partner
   */
  List<PartnerOptionDto> findAllPartnerOption(int partnerId);

}
