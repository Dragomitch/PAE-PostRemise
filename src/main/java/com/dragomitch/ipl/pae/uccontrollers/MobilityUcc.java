package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;

import java.util.Map;

public interface MobilityUcc {

  /**
   * Return a list of all stored mobilities.
   * 
   * @param userId the id of the requester
   * @param userRole the role of the requester
   */
  Map<String, Object> showAll(int userId, String userRole);

  /**
   * Return the requested mobility.
   * 
   * @param id the id of the mobility to be returned
   * @param role the role of the requester
   * @param user the id of the requester
   * @return the requested mobility
   * @throws RessourceNotFoundException when the requested mobility does not exist
   * @throws InsufficientPermissionException when the requester is not a professor and does not own
   *         the requested mobility
   */
  MobilityDto showOne(int id, String role, int user);

  /**
   * Confirm the encoding in Pro Eco software.
   *
   * @param id the mobility to be updated
   * @param version the current version of the mobility to update
   * @throws RessourceNotFoundException if the mobility does not exist
   */
  void confirmProEcoEncoding(int id, int version);

  /**
   * Confirm the encoding in Second software.
   *
   * @param id the mobility to be updated
   * @param version the current version of the mobility to update
   * @throws RessourceNotFoundException if the mobility does not exist
   */
  void confirmSecondSoftwareEncoding(int id, int version);

  /**
   * Confirm a payment.
   *
   * @param id the mobility to update
   * @param version the current version of the mobility to update
   * @return the updated mobility
   * @throws RessourceNotFoundException if the mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  MobilityDto confirmPayment(int id, int version);

  /**
   * Confirm the retrieval of a document.
   * 
   * @param id the mobility to update
   * @param document the retrieved document document
   * @return the updated mobility
   * @throws RessourceNotFoundException if this mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  MobilityDto confirmDocument(int id, int document, int version);

  /**
   * Cancel a mobility. If the requester is a professor, he can cancel a mobility using a reusable
   * denial reason. Otherwise the requester provides a cancellation reason.
   * 
   * @param id the mobility to update
   * @param cancellationReason the cancellation reason
   * @param denialReasonId the denial reason id
   * @param userId the requester id
   * @param userRole the requester role
   * @return the updated mobility
   * @throws RessourceNotFoundException if this mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  MobilityDto cancel(int id, int version, String cancellationReason, int denialReasonId, int userId,
      String userRole);

  /**
   * Create a CSV file as a String with the specified type of documents, for the mobility specified.
   * 
   * @param mobilityId the id of the mobility we want's to export the documents
   * @param filter a filter for the documents to export
   * @return the string representing the CSV file.
   */
  String exportDocuments(int mobilityId, String filter);

}
