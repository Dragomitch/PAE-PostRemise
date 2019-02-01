package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;

import java.util.List;

public interface NominatedStudentDao {

  String TABLE_NAME = "nominated_students";
  String COLUMN_ID = "user_id";
  String COLUMN_TITLE = "title";
  String COLUMN_BIRTHDATE = "birthdate";
  String COLUMN_NATIONALITY = "nationality";
  String COLUMN_ADDRESS = "address";
  String COLUMN_PHONE_NUMBER = "phone_number";
  String COLUMN_GENDER = "gender";
  String COLUMN_PASSED_YEARS_COUNT = "passed_years_count";
  String COLUMN_IBAN = "iban";
  String COLUMN_CARD_HOLDER = "card_holder";
  String COLUMN_BANK_NAME = "bank_name";
  String COLUMN_BIC = "bic";

  int TITLE_MAX_LENGTH = 3;
  int PHONE_NUMBER_MAX_LENGTH = 15;
  int CARD_HOLDER_MAX_LENGTH = 35;
  int BANK_NAME_MAX_LENGTH = 60;
  int BIC_LENGTH_1 = 8; // soit 8, soit 11
  int BIC_LENGTH_2 = 11;
  int IBAN_MAX_LENGTH = 31; // devrait etre 34 ?

  /**
   * Inserts a nominatedStudent into the database.
   * 
   * @param nominatedStudent , the nominatedStudent to be inserted in the database.
   * @return the nominatedStudent inserted in the database
   */
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent);

  /**
   * Queries the database for a nominatedStudent based on it's id.
   * 
   * @param id : the id of the nominatedStudent we want to find.
   * @return a nominatedStudentDto containing the data we were looking for.
   */
  public NominatedStudentDto findById(int id);

  /**
   * Queries the database for all nominatedStudents.
   * 
   * @return a list of all nominatedStudent.
   */
  public List<NominatedStudentDto> findAll();

  /**
   * Update a nominatedStudent in the database.
   * 
   * @param nominatedStudent : the nominatedStudent to be updated in the database.
   * @return the nominatedStudentDto that has just been updated.
   */
  public NominatedStudentDto update(NominatedStudentDto nominatedStudent);

}
