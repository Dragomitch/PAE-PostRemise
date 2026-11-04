package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.CountryDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.OptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Programme;
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

  public ReferenceDataMapper() {}

  static <S, T> List<T> list(List<S> sources, Function<S, T> mapper) {
    return sources.stream().map(mapper).toList();
  }

  public @Nullable OptionDto toDto(@Nullable Option option) {
    return option == null ? null : new OptionDto(option.getCode(), option.getName());
  }

  public List<OptionDto> toOptionDtos(List<Option> options) {
    return list(options, this::toDto);
  }

  public @Nullable Option toDomain(@Nullable OptionDto dto) {
    if (dto == null) {
      return null;
    }
    var option = new Option();
    option.setCode(dto.code());
    option.setName(dto.name());
    return option;
  }

  public @Nullable ProgrammeDto toDto(@Nullable Programme programme) {
    return programme == null
        ? null
        : new ProgrammeDto(
            programme.getId(), programme.getProgrammeName(), programme.getExternalSoftName());
  }

  public List<ProgrammeDto> toProgrammeDtos(List<Programme> programmes) {
    return list(programmes, this::toDto);
  }

  public @Nullable Programme toDomain(@Nullable ProgrammeDto dto) {
    if (dto == null) {
      return null;
    }
    var programme = new Programme();
    programme.setId(dto.id());
    programme.setProgrammeName(dto.programmeName());
    programme.setExternalSoftName(dto.externalSoftName());
    return programme;
  }

  public @Nullable CountryDto toDto(@Nullable Country country) {
    return country == null
        ? null
        : new CountryDto(
            country.getCountryCode(),
            country.getName(),
            toDto(country.getProgramme()),
            country.getVersion());
  }

  public List<CountryDto> toCountryDtos(List<Country> countries) {
    return list(countries, this::toDto);
  }

  public @Nullable Country toDomain(@Nullable CountryDto dto) {
    if (dto == null) {
      return null;
    }
    var country = new Country();
    country.setCountryCode(dto.countryCode());
    country.setName(dto.name());
    country.setProgramme(toDomain(dto.programme()));
    country.setVersion(dto.version());
    return country;
  }

  public @Nullable DenialReasonDto toDto(@Nullable DenialReason reason) {
    return reason == null
        ? null
        : new DenialReasonDto(reason.getId(), reason.getReason(), reason.getVersion());
  }

  public List<DenialReasonDto> toDenialReasonDtos(List<DenialReason> reasons) {
    return list(reasons, this::toDto);
  }

  public @Nullable DenialReason toDomain(@Nullable DenialReasonDto dto) {
    if (dto == null) {
      return null;
    }
    var reason = new DenialReason();
    reason.setId(dto.id());
    reason.setReason(dto.reason());
    reason.setVersion(dto.version());
    return reason;
  }
}
