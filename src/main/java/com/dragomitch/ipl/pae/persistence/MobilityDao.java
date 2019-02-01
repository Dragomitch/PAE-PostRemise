package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.MobilityDto;

import java.util.List;

public interface MobilityDao {

  String TABLE_NAME = "mobilities";
  String COLUMN_ID = "mobility_choice_id";
  String COLUMN_SUBMISSION_DATE = "submission_date";
  String COLUMN_STATE = "state";
  String COLUMN_STATE_BEFORE_CANCELLATION = "state_before_cancellation";
  String COLUMN_FIRST_PAYMENT_REQUEST_DATE = "first_payment_request_date";
  String COLUMN_SECOND_PAYMENT_REQUEST_DATE = "second_payment_request_date";
  String COLUMN_PRO_ECO_ENCODING = "pro_eco_encoding";
  String COLUMN_SECOND_SOFTWARE_ENCODING = "second_software_encoding";
  String COLUMN_STUDENT_CANCELLATION_REASON = "student_cancellation_reason";
  String COLUMN_PROF_DENIAL_REASON = "prof_denial_reason";
  String COLUMN_PROFESSOR_IN_CHARGE = "professor_in_charge";


  /**
   * Inserts a mobility in db.
   * 
   * @param mobility the mobility to insert
   * @return the created mobility
   */
  MobilityDto create(MobilityDto mobility);

  /**
   * Finds a mobility in db.
   * 
   * @param id the mobility id to be found
   * @return the mobility found, null if no mobility has been found
   */
  MobilityDto findById(int id);

  /**
   * Lists all mobilities stored in db.
   * 
   * @return a list of all mobilities
   */
  List<MobilityDto> findAll();

  /**
   * Lists all mobilities owned by a user.
   * 
   * @param user the user id
   * @return a list of mobilities
   */
  List<MobilityDto> findByUser(int user);

  /**
   * Updates a mobility in db.
   * 
   * @param mobility the mobility to update
   * @return the updated mobility
   */
  MobilityDto update(MobilityDto mobility);

}
