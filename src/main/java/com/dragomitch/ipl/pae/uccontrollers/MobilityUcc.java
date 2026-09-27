package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface MobilityUcc {

  /**
   * Filters of the document export: departure documents (D), filled-in departure documents (DF),
   * return documents (R), filled-in return documents (RF); every document without a filter.
   */
  String DOCUMENT_FILTER_REGEXP = "D|DF|R|RF";
  /** Message of an unknown document filter. */
  String DOCUMENT_FILTER_MESSAGE = "{pae.validation.MobilityUcc.documentFilter.message}";

  /**
   * Return a list of all stored mobilities.
   * 
   * @param userId the id of the requester
   * @param userRole the role of the requester
   * @return every mobility for a professor, the requester's own ones for a student
   */
  List<MobilityDto> showAll(@Positive int userId, @NotBlank String userRole);

  /**
   * Return the requested mobility.
   * 
   * @param id the id of the mobility to be returned
   * @param role the role of the requester
   * @param user the id of the requester
   * @return the requested mobility
   * @throws ResourceNotFoundException when the requested mobility does not exist
   * @throws InsufficientPermissionException when the requester is not a professor and does not own
   *         the requested mobility
   */
  MobilityDto showOne(@Positive int id, @NotBlank String role, @Positive int user);

  /**
   * Confirm the encoding in Pro Eco software.
   *
   * @param id the mobility to be updated
   * @param version the current version of the mobility to update
   * @throws ResourceNotFoundException if the mobility does not exist
   */
  void confirmProEcoEncoding(@Positive int id, int version);

  /**
   * Confirm the encoding in Second software.
   *
   * @param id the mobility to be updated
   * @param version the current version of the mobility to update
   * @throws ResourceNotFoundException if the mobility does not exist
   */
  void confirmSecondSoftwareEncoding(@Positive int id, int version);

  /**
   * Confirm a payment.
   *
   * @param id the mobility to update
   * @param version the current version of the mobility to update
   * @return the updated mobility
   * @throws ResourceNotFoundException if the mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  MobilityDto confirmPayment(@Positive int id, int version);

  /**
   * Confirm the retrieval of a document.
   * 
   * @param id the mobility to update
   * @param document the retrieved document document
   * @return the updated mobility
   * @throws ResourceNotFoundException if this mobility does not exist
   * @throws BusinessException if a business-related operation error occurs.
   */
  MobilityDto confirmDocument(@Positive int id, @Positive int document, int version);

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
  MobilityDto cancel(@Positive int id, int version,
      @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH) String cancellationReason,
      int denialReasonId, @Positive int userId, @NotBlank String userRole);

  /**
   * Create a CSV file as a String with the specified type of documents, for the mobility specified.
   * 
   * @param mobilityId the id of the mobility we want's to export the documents
   * @param filter a filter for the documents to export
   * @return the string representing the CSV file.
   */
  String exportDocuments(@Positive int mobilityId,
      @Pattern(regexp = DOCUMENT_FILTER_REGEXP, message = DOCUMENT_FILTER_MESSAGE) String filter);

}
