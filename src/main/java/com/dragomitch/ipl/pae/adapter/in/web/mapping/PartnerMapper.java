package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Maps the partners and the options they offer between the web DTOs and the domain. */
@Component
public class PartnerMapper {

  private final ReferenceDataMapper referenceData;
  private final AddressMapper addresses;

  public PartnerMapper(ReferenceDataMapper referenceData, AddressMapper addresses) {
    this.referenceData = referenceData;
    this.addresses = addresses;
  }

  public @Nullable PartnerOptionDto toDto(@Nullable PartnerOption option) {
    return option == null
        ? null
        : new PartnerOptionDto(option.getCode(), option.getName(), option.getDepartement());
  }

  public List<PartnerOptionDto> toPartnerOptionDtos(List<PartnerOption> options) {
    return ReferenceDataMapper.list(options, this::toDto);
  }

  public @Nullable PartnerOption toDomain(@Nullable PartnerOptionDto dto) {
    if (dto == null) {
      return null;
    }
    var option = new PartnerOption();
    option.setCode(dto.code());
    option.setName(dto.name());
    option.setDepartement(dto.departement());
    return option;
  }

  public @Nullable PartnerDto toDto(@Nullable Partner partner) {
    return partner == null
        ? null
        : new PartnerDto(
            partner.getId(),
            partner.getLegalName(),
            partner.getBusinessName(),
            partner.getFullName(),
            partner.getOrganisationType(),
            partner.getEmployeeCount(),
            addresses.toDto(partner.getAddress()),
            partner.getEmail(),
            partner.getWebsite(),
            partner.getPhoneNumber(),
            partner.isOfficial(),
            partner.isArchived(),
            partner.isArchivable(),
            partner.getOptions() == null ? null : toPartnerOptionDtos(partner.getOptions()),
            referenceData.toDto(partner.getProgramme()),
            partner.getVersion());
  }

  public List<PartnerDto> toPartnerDtos(List<Partner> partners) {
    return ReferenceDataMapper.list(partners, this::toDto);
  }

  public @Nullable Partner toDomain(@Nullable PartnerDto dto) {
    if (dto == null) {
      return null;
    }
    var partner = new Partner();
    partner.setId(dto.id());
    partner.setLegalName(dto.legalName());
    partner.setBusinessName(dto.businessName());
    partner.setFullName(dto.fullName());
    partner.setOrganisationType(dto.organisationType());
    partner.setEmployeeCount(dto.employeeCount());
    partner.setAddress(addresses.toDomain(dto.address()));
    partner.setEmail(dto.email());
    partner.setWebsite(dto.website());
    partner.setPhoneNumber(dto.phoneNumber());
    partner.setOfficial(dto.official());
    partner.setArchived(dto.archived());
    partner.setArchivable(dto.archivable());
    if (dto.options() != null) {
      List<PartnerOption> options = new ArrayList<>();
      dto.options().forEach(option -> options.add(toDomain(option)));
      partner.setOptions(options);
    }
    partner.setProgramme(referenceData.toDomain(dto.programme()));
    partner.setVersion(dto.version());
    return partner;
  }
}
