package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;

import java.util.Map;

public interface MobilityChoiceUcc {

  /**
   * Creates a new MobilityChoice with the parameters received from the user.
   * 
   * @param mobilityChoice a MobilityChoiceDto Object with all the information provided by the user.
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @return the new MobilityChoiceDto for the object created in DataBase.
   */
  MobilityChoiceDto create(MobilityChoiceDto mobilityChoice, int userId, String userRole);

  /**
   * Returns a list with all the mobilityChoices stocked in DataBase.
   * 
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to display
   * @return A list with all the mobilityChoices in DataBase as mobilityChoicesDto's.
   */
  Map<String, Object> showAll(int userId, String userRole, String filter);


  /**
   * Returns a list with all the mobilityChoices stocked in DataBase AND the number of results.
   * 
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to display
   * @return A list with all the mobilityChoices as mobilityChoicesDto's and their count.
   */
  Map<String, Object> countAll(int userId, String userRole, String filter);

  /**
   * Confirms the mobility choice which bears the given id.
   * 
   * @param mobilityChoiceId the mobility choice id
   * @param userId the userId of the professor confirming the mobility choice
   */
  void confirm(int mobilityChoiceId, int userId);

  /**
   * Confirms the mobility choice which bears the given id with a new partner.
   * 
   * @param mobilityChoiceId the mobility choice id
   * @param partner the new partner
   * @param userId the userId of the student or professor confirming the mobility choice
   * @param userRole the role of the currently signed in user
   */
  void confirmWithNewPartner(int mobilityChoiceId, PartnerDto partner, int userId, String userRole);

  /**
   * Cancels a MobilityChoice specified by mobilityChoiceId.
   * 
   * @param mobilityChoiceId the id of the MobilityChoice the user want to cancel.
   * @param userId the id of the user connected.
   * @param reason the reason provided by the user for canceling the MobilityChoice.
   */
  void cancel(int mobilityChoiceId, int userId, String reason);

  /**
   * Reject a MobilityChoice specified by mobilityChoiceId.
   * 
   * @param mobilityChoiceId the id of the MobilityChoice the professor wants to reject.
   * @param reasonId the id of the reason provided by the professor for rejecting the
   *        MobilityChoice.
   */
  void reject(int mobilityChoiceId, int reasonId);

  /**
   * Create a CSV file as a string with the specified type of mobility Choices
   * 
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to export
   * @return the string representing the CSV file.
   */
  String exportAll(int userId, String userRole, String filter);

  /**
   * Check if there is a mobilityChoice attached to a partner.
   * 
   * @param partnerId the id of the partner for which we want to check.
   */
  boolean findByPartner(int partnerId);

}
