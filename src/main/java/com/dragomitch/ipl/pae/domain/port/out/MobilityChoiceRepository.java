package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import java.util.List;

public interface MobilityChoiceRepository {
  String TABLE_NAME = "mobility_choices";
  String COLUMN_ID = "mobility_choice_id";
  String COLUMN_USER_ID = "user_id";
  String COLUMN_PREFERENCE_ORDER = "preference_order";
  String COLUMN_MOBILITY_TYPE = "mobility_type";
  String COLUMN_ACADEMIC_YEAR = "academic_year";
  String COLUMN_TERM = "term";
  String COLUMN_PROGRAMME = "programme";
  String COLUMN_COUNTRY = "country";
  String COLUMN_SUBMISSION_DATE = "submission_date";
  String COLUMN_PROF_DENIAL_REASON = "prof_denial_reason";
  String COLUMN_STUDENT_CANCELLATION_REASON = "student_cancellation_reason";
  String COLUMN_PARTNER = "partner";

  int MAX_LENGTH_MOBILITY_TYPE = 3;
  int MAX_LENGTH_COUNTRY = 2;
  int MAX_LENGTH_STUDENT_CANCELLATION_REASON = 500;

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
   * @return the mobilityChoiceDto of the mobility choice we were looking for.
   */
  MobilityChoice findById(int mobilityChoiceId);

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
