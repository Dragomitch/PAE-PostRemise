package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;

import java.util.List;

public interface MobilityChoiceDao {
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

  String FILTER_ALL_MOBILITIES_CHOICES = "all";
  String FILTER_ACTIVE_MOBILITIES_CHOICES = "active";
  String FILTER_CANCELED_MOBILITIES_CHOICES = "canceled";
  String FILTER_REJECTED_MOBILITIES_CHOICES = "rejected";
  String FILTER_PASSED_MOBILITIES_CHOICES = "passed";


  /**
   * Inserts a Mobility choice in the Database.
   * 
   * @param mobilityChoice the mobility choice to create.
   * @return the mobilityChoice created.
   */
  MobilityChoiceDto create(MobilityChoiceDto mobilityChoice);

  /**
   * Queries the database to find the mobilityChoice where the id is mobilityChoiceId.
   * 
   * @param mobilityChoiceId the id we are looking for.
   * @return the mobilityChoiceDto of the mobility choice we were looking for.
   */
  MobilityChoiceDto findById(int mobilityChoiceId);

  /**
   * @param userId the user's id to search the mobilities choices for.
   * @return a list of mobility choices for the specified user
   */
  List<MobilityChoiceDto> findByUser(int userId);

  /**
   * @param partnerId the partner's id to search the mobilities choices for.
   * @return a list of mobility choices for the specified partner
   */
  List<MobilityChoiceDto> findByPartner(int partnerId);

  /**
   * Return the partners currently selected for a mobility or a mobilityChoice.
   * 
   * @param partnerId the partner's id to search the mobilities choices for.
   * @return a list of mobility choices for the specified partner
   */
  List<MobilityChoiceDto> findByActivePartner(int partnerId);

  /**
   * Queries the database for all mobility choices.
   * 
   * @param filter the filter we want to apply to the query. The filter must be one of those:
   *        passed, active, canceled, rejected, all. Combinations are NOT possible between the
   *        filters.
   * @return all the mobility choices fitting the filter.
   */
  List<MobilityChoiceDto> findAll(String filter);

  /**
   * Update a mobilityChoice into the DB with the new fields of the MobilityChoice.
   * 
   * @param mobilityChoice the mobility choice we are updating.
   */
  void update(MobilityChoiceDto mobilityChoice);

}
