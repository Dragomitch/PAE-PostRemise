package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;
import java.util.Optional;

public interface PartnerRepository {

  /**
   * Inserts a partner into the database. A partner without website ({@code null}) is stored with an
   * empty one, as the website is optional; the same holds for {@link #update}.
   *
   * @param partner : the partner to be inserted in the database
   * @return the Partner that has just been inserted
   */
  Partner create(Partner partner);

  /**
   * Queries the database for a partner based on it's partner_id.
   *
   * @param id : the partner_id of the partner we want to find
   * @return the Partner containing the data we were looking for, with all its options (an empty
   *     list if it has none), or empty if there is no such partner
   */
  Optional<Partner> findById(int id);

  /**
   * Queries the database for all partners matching a filter, each with all its options.
   *
   * @param filter : the filter we want to apply: {@link PartnerFilter#ALL}, {@link
   *     PartnerFilter#COUNTRY} (country code {@code value}) or {@link PartnerFilter#ARCHIVED}
   *     (archived partners whose full name contains {@code value}, case-insensitively).
   * @param value : the value of the filter.
   * @param userRole : the role of the user; students only get the official, non-archived partners
   *     offering their option (except with the archived filter).
   * @param option : the option of the user.
   * @return a List of all Partner, ordered by id
   */
  List<Partner> findAll(PartnerFilter filter, String value, Role userRole, String option);

  /**
   * Update a partner in the database.
   *
   * @param partner : the partner to be updated in the database
   * @return the partner that has just been updated
   */
  Partner update(Partner partner);
}
