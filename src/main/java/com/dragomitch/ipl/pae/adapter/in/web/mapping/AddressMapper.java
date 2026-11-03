package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Maps the addresses between the web DTOs and the domain. */
@Component
public class AddressMapper {

  private final EntityFactory entityFactory;
  private final ReferenceDataMapper referenceData;

  public AddressMapper(EntityFactory entityFactory, ReferenceDataMapper referenceData) {
    this.entityFactory = entityFactory;
    this.referenceData = referenceData;
  }

  public @Nullable AddressDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable AddressDto address) {
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

  public com.dragomitch.ipl.pae.domain.dto.@Nullable AddressDto toDomain(@Nullable AddressDto dto) {
    if (dto == null) {
      return null;
    }
    var address =
        (com.dragomitch.ipl.pae.domain.dto.AddressDto)
            entityFactory.build(com.dragomitch.ipl.pae.domain.dto.AddressDto.class);
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
