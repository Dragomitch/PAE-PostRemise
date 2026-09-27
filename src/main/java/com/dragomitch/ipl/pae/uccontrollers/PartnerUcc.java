package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PartnerUcc {

  /**
   * Create a partner.
   * 
   * @param partner : the partnerDto coming from the front-end
   * @param userRole : the role of the user who wants to create a new partner
   * @return the partnerDto that has just been created
   */
  @Validated({Default.class, OnCreate.class})
  PartnerDto create(@NotNull @Valid PartnerDto partner, @NotBlank String userRole);

  /**
   * Find a partnerDto existing in the database.
   * 
   * @param id : the id of the partner we want to find
   * @return the partnerDto found in the database
   */
  PartnerDto showOne(@Positive int id);

  /**
   * Find all PartnerDao.
   * 
   * @param filter : the filter to apply
   * @param value : the value of the filter
   * @param userId : the id of the user who wants to edit a partner.
   * @param userRole : the role of the user who wants to edit a partner.
   * @return a list of all the partnerDto found in the database
   */
  List<PartnerDto> showAll(@NotNull @Valid PartnerSearch search, @NotBlank String userRole,
      @Positive int userId);

  /**
   * Edit a partner.
   * 
   * @param id : the id of the partnerDto we want to edit coming from the front-end.
   * @param partner : the partnerDto containing the new data.
   * @param userRole : the role of the user who wants to edit a partner.
   * @return the partnerDto that has just been edited.
   */
  PartnerDto edit(@Positive int id, @NotNull @Valid PartnerDto partner,
      @NotBlank String userRole);

  PartnerDto restore(@Positive int id, @NotBlank String role);

  /**
   * Add an option to a partner.
   * 
   * @param id : the id of the partner for which we want to add an option.
   * @param partnerOption : the partnerOption of the we want to add.
   */
  void addOption(@Positive int id, @NotNull @Valid PartnerOptionDto partnerOption);

  /**
   * Find all options for a partner.
   * 
   * @param partnerId : the id of the partnerDto for which we want to find options
   * @return a list of options corresponding to a partner
   */
  List<PartnerOptionDto> findAllPartnerOption(@Positive int partnerId);

}
