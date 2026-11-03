package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PartnerService {

  /**
   * Create a partner.
   *
   * @param partner : the partnerDto coming from the front-end
   * @param userRole : the role of the user who wants to create a new partner
   * @return the partnerDto that has just been created
   */
  @Validated({Default.class, OnCreate.class})
  PartnerDto create(@NotNull @Valid PartnerDto partner, @NotNull Role userRole);

  /**
   * Find a partnerDto existing in the database.
   *
   * @param id : the id of the partner we want to find
   * @return the partnerDto found in the database
   */
  PartnerDto showOne(@Positive int id);

  /**
   * The partners visible to the user, filtered.
   *
   * @param filter the filter to apply
   * @param value the value of the filter: part of the name for {@link PartnerFilter#ARCHIVED}, a
   *     country code for {@link PartnerFilter#COUNTRY}, ignored for {@link PartnerFilter#ALL}
   * @param userRole the role of the requester
   * @param userId the id of the requester
   * @return the partners, ordered by id
   */
  List<PartnerDto> showAll(
      @NotNull PartnerFilter filter,
      @Nullable String value,
      @NotNull Role userRole,
      @Positive int userId);

  /**
   * Edit a partner.
   *
   * @param id : the id of the partnerDto we want to edit coming from the front-end.
   * @param partner : the partnerDto containing the new data.
   * @param userRole : the role of the user who wants to edit a partner.
   * @return the partnerDto that has just been edited.
   */
  PartnerDto edit(@Positive int id, @NotNull @Valid PartnerDto partner, @NotNull Role userRole);

  PartnerDto restore(@Positive int id, @NotNull Role role);

  /**
   * Adds an option to a partner. A professor may add an option to any partner. A student may only
   * complete a non-official partner he uses, i.e. the partner of one of his own mobility choices or
   * mobilities (a student creates such a partner when he confirms a choice with a new partner);
   * official partners are managed by the professors.
   *
   * @param id the id of the partner
   * @param partnerOption the option to add
   * @param userId the id of the requester
   * @param userRole the role of the requester
   * @throws com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException if there is no such
   *     partner (404)
   * @throws com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException if a student
   *     may not change that partner (403 {@code ACCESS_DENIED})
   */
  void addOption(
      @Positive int id,
      @NotNull @Valid PartnerOptionDto partnerOption,
      @Positive int userId,
      @NotNull Role userRole);

  /**
   * Find all options for a partner.
   *
   * @param partnerId : the id of the partnerDto for which we want to find options
   * @return a list of options corresponding to a partner
   */
  List<PartnerOptionDto> findAllPartnerOption(@Positive int partnerId);
}
