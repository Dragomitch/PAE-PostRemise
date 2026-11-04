package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.DocumentFilter;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface MobilityService {

  /**
   * Return a list of all stored mobilities.
   *
   * @param userId the id of the requester
   * @param userRole the role of the requester
   * @return every mobility for a professor, the requester's own ones for a student
   */
  List<Mobility> showAll(int userId, Role userRole);

  /**
   * Return the requested mobility.
   *
   * @param id the id of the mobility to be returned
   * @param role the role of the requester
   * @param user the id of the requester
   * @return the requested mobility
   * @throws ResourceNotFoundException when the requested mobility does not exist
   * @throws InsufficientPermissionException when the requester is not a professor and does not own
   *     the requested mobility
   */
  Mobility showOne(int id, Role role, int user);

  /**
   * Confirm the encoding in Pro Eco software.
   *
   * @param id the mobility to be updated
   * @param version the current version of the mobility to update
   * @throws ResourceNotFoundException if the mobility does not exist
   */
  void confirmProEcoEncoding(int id, int version);

  /**
   * Confirm the encoding in Second software.
   *
   * @param id the mobility to be updated
   * @param version the current version of the mobility to update
   * @throws ResourceNotFoundException if the mobility does not exist
   */
  void confirmSecondSoftwareEncoding(int id, int version);

  /**
   * Confirm a payment.
   *
   * @param id the mobility to update
   * @param version the current version of the mobility to update
   * @return the updated mobility
   * @throws ResourceNotFoundException if the mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  Mobility confirmPayment(int id, int version);

  /**
   * Confirm the retrieval of a document.
   *
   * @param id the mobility to update
   * @param document the retrieved document document
   * @return the updated mobility
   * @throws ResourceNotFoundException if this mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  Mobility confirmDocument(int id, int document, int version);

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
   * @throws ResourceNotFoundException if this mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  Mobility cancel(
      int id,
      int version,
      String cancellationReason,
      int denialReasonId,
      int userId,
      Role userRole);

  /**
   * Create a CSV file as a String with the specified type of documents, for the mobility specified.
   *
   * @param mobilityId the id of the mobility we want's to export the documents
   * @param filter the documents to export, every document if null
   * @return the string representing the CSV file.
   */
  String exportDocuments(int mobilityId, @Nullable DocumentFilter filter);
}
