package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import java.util.List;
import java.util.Optional;

public interface MobilityChoiceRepository {

  /**
   * Inserts a Mobility choice in the Database.
   *
   * @param mobilityChoice the mobility choice to create.
   * @return the mobilityChoice created.
   */
  MobilityChoice create(MobilityChoice mobilityChoice);

  /**
   * Queries the database to find the mobilityChoice where the id is mobilityChoiceId.
   *
   * @param mobilityChoiceId the id we are looking for.
   * @return the mobilityChoice of the mobility choice we were looking for, or empty if there is
   *     none
   */
  Optional<MobilityChoice> findById(int mobilityChoiceId);

  /**
   * @param userId the user's id to search the mobilities choices for.
   * @return a list of mobility choices for the specified user
   */
  List<MobilityChoice> findByUser(int userId);

  /**
   * @param partnerId the partner's id to search the mobilities choices for.
   * @return a list of mobility choices for the specified partner
   */
  List<MobilityChoice> findByPartner(int partnerId);

  /**
   * Return the choices of the current academic year that select that partner and are neither denied
   * nor cancelled, including those that became a mobility unless that mobility was cancelled (by
   * the student or by a professor). A partner with such choices cannot be archived.
   *
   * @param partnerId the partner's id to search the mobilities choices for.
   * @return a list of mobility choices for the specified partner
   */
  List<MobilityChoice> findByActivePartner(int partnerId);

  /**
   * Queries the database for all mobility choices.
   *
   * @param filter the list to return (one filter, no combination)
   * @return the mobility choices of the filter, ordered by id
   */
  List<MobilityChoice> findAll(MobilityChoiceFilter filter);

  /**
   * Update a mobilityChoice into the DB with the new fields of the MobilityChoice, with optimistic
   * locking; the DTO receives the new version.
   *
   * <p>An empty cancellation reason is stored as NULL on purpose: NULL means "not cancelled" for
   * the filters ({@link MobilityChoiceFilter#CANCELED} is "reason IS NOT NULL"), so an empty text
   * would turn the choice into a cancelled one without reason.
   *
   * @param mobilityChoice the mobility choice we are updating.
   * @throws java.util.ConcurrentModificationException if no choice has that id and version
   */
  void update(MobilityChoice mobilityChoice);
}
