package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.model.Address;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Maps the addresses between the web DTOs and the domain. */
@Component
public class AddressMapper {

  private final ReferenceDataMapper referenceData;

  public AddressMapper(ReferenceDataMapper referenceData) {
    this.referenceData = referenceData;
  }

  public @Nullable AddressDto toDto(@Nullable Address address) {
    return address == null
        ? null
        : new AddressDto(
            address.getId(),
            address.getStreet(),
            address.getNumber(),
            referenceData.toDto(address.getCountry()),
            address.getCity(),
            address.getPostalCode(),
            address.getRegion(),
            address.getVersion());
  }

  public @Nullable Address toDomain(@Nullable AddressDto dto) {
    if (dto == null) {
      return null;
    }
    var address = new Address();
    address.setId(dto.id());
    address.setStreet(dto.street());
    address.setNumber(dto.number());
    address.setCountry(referenceData.toDomain(dto.country()));
    address.setCity(dto.city());
    address.setPostalCode(dto.postalCode());
    address.setRegion(dto.region());
    address.setVersion(dto.version());
    return address;
  }
}
