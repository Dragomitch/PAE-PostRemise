package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;

public interface MobilityChoiceService {

  /**
   * Creates a new MobilityChoice with the parameters received from the user.
   *
   * @param mobilityChoice a MobilityChoice Object with all the information provided by the user.
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @return the new MobilityChoice for the object created in DataBase.
   */
  MobilityChoice create(MobilityChoice mobilityChoice, int userId, Role userRole);

  /**
   * Returns a list with all the mobilityChoices stocked in DataBase.
   *
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to display
   * @return A list with all the mobilityChoices in DataBase as mobilityChoicesDto's.
   */
  List<MobilityChoice> showAll(int userId, Role userRole, MobilityChoiceFilter filter);

  /**
   * Counts the mobility choices {@link #showAll} would return.
   *
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to display
   * @return the number of mobility choices
   */
  int countAll(int userId, Role userRole, MobilityChoiceFilter filter);

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
  void confirmWithNewPartner(int mobilityChoiceId, Partner partner, int userId, Role userRole);

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
   *     MobilityChoice.
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
  String exportAll(int userId, Role userRole, MobilityChoiceFilter filter);

  /**
   * Check if there is a mobilityChoice attached to a partner.
   *
   * @param partnerId the id of the partner for which we want to check.
   */
  boolean findByPartner(int partnerId);
}
