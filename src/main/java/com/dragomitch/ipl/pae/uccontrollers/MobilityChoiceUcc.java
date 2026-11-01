package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface MobilityChoiceUcc {

  /** Filters of the mobility choice lists: all, active (default), canceled, rejected, passed. */
  String FILTER_REGEXP = "all|active|canceled|rejected|passed";
  /** Message of an unknown filter. */
  String FILTER_MESSAGE = "{pae.validation.MobilityChoiceUcc.filter.message}";

  /**
   * Creates a new MobilityChoice with the parameters received from the user.
   * 
   * @param mobilityChoice a MobilityChoiceDto Object with all the information provided by the user.
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @return the new MobilityChoiceDto for the object created in DataBase.
   */
  MobilityChoiceDto create(@NotNull @Valid MobilityChoiceDto mobilityChoice, @Positive int userId,
      @NotBlank String userRole);

  /**
   * Returns a list with all the mobilityChoices stocked in DataBase.
   * 
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to display
   * @return A list with all the mobilityChoices in DataBase as mobilityChoicesDto's.
   */
  List<MobilityChoiceDto> showAll(@Positive int userId, @NotBlank String userRole,
      @Pattern(regexp = FILTER_REGEXP, message = FILTER_MESSAGE) String filter);


  /**
   * Counts the mobility choices {@link #showAll} would return.
   * 
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to display
   * @return the number of mobility choices
   */
  int countAll(@Positive int userId, @NotBlank String userRole,
      @Pattern(regexp = FILTER_REGEXP, message = FILTER_MESSAGE) String filter);

  /**
   * Confirms the mobility choice which bears the given id.
   * 
   * @param mobilityChoiceId the mobility choice id
   * @param userId the userId of the professor confirming the mobility choice
   */
  void confirm(@Positive int mobilityChoiceId, @Positive int userId);

  /**
   * Confirms the mobility choice which bears the given id with a new partner.
   * 
   * @param mobilityChoiceId the mobility choice id
   * @param partner the new partner
   * @param userId the userId of the student or professor confirming the mobility choice
   * @param userRole the role of the currently signed in user
   */
  void confirmWithNewPartner(@Positive int mobilityChoiceId, @NotNull @Valid PartnerDto partner,
      @Positive int userId, @NotBlank String userRole);

  /**
   * Cancels a MobilityChoice specified by mobilityChoiceId.
   * 
   * @param mobilityChoiceId the id of the MobilityChoice the user want to cancel.
   * @param userId the id of the user connected.
   * @param reason the reason provided by the user for canceling the MobilityChoice.
   */
  void cancel(@Positive int mobilityChoiceId, @Positive int userId,
      @NotBlank @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH) String reason);

  /**
   * Reject a MobilityChoice specified by mobilityChoiceId.
   * 
   * @param mobilityChoiceId the id of the MobilityChoice the professor wants to reject.
   * @param reasonId the id of the reason provided by the professor for rejecting the
   *        MobilityChoice.
   */
  void reject(@Positive int mobilityChoiceId, @Positive int reasonId);

  /**
   * Create a CSV file as a string with the specified type of mobility Choices
   * 
   * @param userId the id of the user connected.
   * @param userRole the role of the user who's sending the request.
   * @param filter a filter for the mobility choices to export
   * @return the string representing the CSV file.
   */
  String exportAll(@Positive int userId, @NotBlank String userRole,
      @Pattern(regexp = FILTER_REGEXP, message = FILTER_MESSAGE) String filter);

  /**
   * Check if there is a mobilityChoice attached to a partner.
   * 
   * @param partnerId the id of the partner for which we want to check.
   */
  boolean findByPartner(@Positive int partnerId);

}
