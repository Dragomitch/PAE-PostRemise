package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.domain.validation.PhoneNumber;
import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A partner institution or company. Constraints: {@link jakarta.validation.groups.Default} for an
 * edition, {@code Default} and {@link OnCreate} (at least one option) for a creation. Lengths:
 * {@code partners}.
 *
 * @param id the id
 * @param legalName the legal name
 * @param businessName the business name
 * @param fullName the full name
 * @param organisationType TPE, PME, ETI or TGE for companies, free text otherwise
 * @param employeeCount the number of employees
 * @param address the address
 * @param email the email address
 * @param website the website, optional
 * @param phoneNumber the phone number
 * @param official whether the partner is official (created by a professor)
 * @param archived whether the partner is archived
 * @param archivable whether the partner may be archived (no active mobility choice), set by the
 *     server
 * @param options the options offered; a new partner offers at least one (an edition only adds
 *     options). A {@code null} element is refused ({@code options[i]}: {@code NotNull}).
 * @param programme the programme of the country of the partner, set by the server
 * @param version the version
 */
public record PartnerDto(
    @Positive(groups = Reference.class) int id,
    @NotBlank @Size(max = PartnerDto.NAME_MAX_LENGTH) String legalName,
    @NotBlank @Size(max = PartnerDto.NAME_MAX_LENGTH) String businessName,
    @NotBlank @Size(max = PartnerDto.NAME_MAX_LENGTH) String fullName,
    @NotBlank @Size(max = PartnerDto.ORGANISATION_TYPE_MAX_LENGTH) String organisationType,
    @Positive int employeeCount,
    @NotNull @Valid AddressDto address,
    @NotBlank @Email(regexp = UserDto.EMAIL_REGEXP) @Size(max = PartnerDto.EMAIL_MAX_LENGTH)
        String email,
    @Size(max = PartnerDto.WEBSITE_MAX_LENGTH) String website,
    @NotBlank @Size(max = PartnerDto.PHONE_NUMBER_MAX_LENGTH) @PhoneNumber String phoneNumber,
    boolean official,
    boolean archived,
    boolean archivable,
    @NotEmpty(groups = OnCreate.class) List<@NotNull @Valid PartnerOptionDto> options,
    ProgrammeDto programme,
    @Schema(description = ApiDocs.VERSION) int version) {

  public static final int NAME_MAX_LENGTH = 255;
  public static final int ORGANISATION_TYPE_MAX_LENGTH = 60;
  public static final int EMAIL_MAX_LENGTH = 255;
  public static final int WEBSITE_MAX_LENGTH = 255;
  public static final int PHONE_NUMBER_MAX_LENGTH = 15;
}
