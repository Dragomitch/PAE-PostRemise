package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface PartnerService {

  /**
   * Create a partner.
   *
   * @param partner : the partner coming from the front-end
   * @param userRole : the role of the user who wants to create a new partner
   * @return the partner that has just been created
   */
  Partner create(Partner partner, Role userRole);

  /**
   * Find a partner existing in the database.
   *
   * @param id : the id of the partner we want to find
   * @return the partner found in the database
   */
  Partner showOne(int id);

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
  List<Partner> showAll(PartnerFilter filter, @Nullable String value, Role userRole, int userId);

  /**
   * Edit a partner.
   *
   * @param id : the id of the partner we want to edit coming from the front-end.
   * @param partner : the partner containing the new data.
   * @param userRole : the role of the user who wants to edit a partner.
   * @return the partner that has just been edited.
   */
  Partner edit(int id, Partner partner, Role userRole);

  Partner restore(int id, Role role);

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
  void addOption(int id, PartnerOption partnerOption, int userId, Role userRole);

  /**
   * Find all options for a partner.
   *
   * @param partnerId : the id of the partner for which we want to find options
   * @return a list of options corresponding to a partner
   */
  List<PartnerOption> findAllPartnerOption(int partnerId);
}
