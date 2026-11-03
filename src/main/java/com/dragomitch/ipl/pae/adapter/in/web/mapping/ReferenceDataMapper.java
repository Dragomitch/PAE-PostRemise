package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.CountryDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.OptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps the reference data (options, programmes, countries, denial reasons) between the web DTOs and
 * the domain. A {@code null} maps to {@code null}: the domain leaves out what a read does not
 * fetch, and the JSON leaves out what is {@code null}.
 */
@Component
public class ReferenceDataMapper {

  private final EntityFactory entityFactory;

  public ReferenceDataMapper(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
  }

  private <T> T build(Class<T> type) {
    return type.cast(entityFactory.build(type));
  }

  static <S, T> List<T> list(List<S> sources, Function<S, T> mapper) {
    return sources.stream().map(mapper).toList();
  }

  public @Nullable OptionDto toDto(com.dragomitch.ipl.pae.domain.dto.@Nullable OptionDto option) {
    return option == null ? null : new OptionDto(option.getCode(), option.getName());
  }

  public List<OptionDto> toOptionDtos(List<com.dragomitch.ipl.pae.domain.dto.OptionDto> options) {
    return list(options, this::toDto);
  }

  public com.dragomitch.ipl.pae.domain.dto.@Nullable OptionDto toDomain(@Nullable OptionDto dto) {
    if (dto == null) {
      return null;
    }
    var option = build(com.dragomitch.ipl.pae.domain.dto.OptionDto.class);
    option.setCode(dto.code());
    option.setName(dto.name());
    return option;
  }

  public @Nullable ProgrammeDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable ProgrammeDto programme) {
    return programme == null
        ? null
        : new ProgrammeDto(
            programme.getId(), programme.getProgrammeName(), programme.getExternalSoftName());
  }

  public List<ProgrammeDto> toProgrammeDtos(
      List<com.dragomitch.ipl.pae.domain.dto.ProgrammeDto> programmes) {
    return list(programmes, this::toDto);
  }

  public com.dragomitch.ipl.pae.domain.dto.@Nullable ProgrammeDto toDomain(
      @Nullable ProgrammeDto dto) {
    if (dto == null) {
      return null;
    }
    var programme = build(com.dragomitch.ipl.pae.domain.dto.ProgrammeDto.class);
    programme.setId(dto.id());
    programme.setProgrammeName(dto.programmeName());
    programme.setExternalSoftName(dto.externalSoftName());
    return programme;
  }

  public @Nullable CountryDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable CountryDto country) {
    return country == null
        ? null
        : new CountryDto(
            country.getCountryCode(),
            country.getName(),
            toDto(country.getProgramme()),
            country.getVersion());
  }

  public List<CountryDto> toCountryDtos(
      List<com.dragomitch.ipl.pae.domain.dto.CountryDto> countries) {
    return list(countries, this::toDto);
  }

  public com.dragomitch.ipl.pae.domain.dto.@Nullable CountryDto toDomain(@Nullable CountryDto dto) {
    if (dto == null) {
      return null;
    }
    var country = build(com.dragomitch.ipl.pae.domain.dto.CountryDto.class);
    country.setCountryCode(dto.countryCode());
    country.setName(dto.name());
    country.setProgramme(toDomain(dto.programme()));
    country.setVersion(dto.version());
    return country;
  }

  public @Nullable DenialReasonDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable DenialReasonDto reason) {
    return reason == null
        ? null
        : new DenialReasonDto(reason.getId(), reason.getReason(), reason.getVersion());
  }

  public List<DenialReasonDto> toDenialReasonDtos(
      List<com.dragomitch.ipl.pae.domain.dto.DenialReasonDto> reasons) {
    return list(reasons, this::toDto);
  }

  public com.dragomitch.ipl.pae.domain.dto.@Nullable DenialReasonDto toDomain(
      @Nullable DenialReasonDto dto) {
    if (dto == null) {
      return null;
    }
    var reason = build(com.dragomitch.ipl.pae.domain.dto.DenialReasonDto.class);
    reason.setId(dto.id());
    reason.setReason(dto.reason());
    reason.setVersion(dto.version());
    return reason;
  }
}
