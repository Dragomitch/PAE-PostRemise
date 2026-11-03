package com.dragomitch.ipl.pae.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.dto.DocumentDto;
import com.dragomitch.ipl.pae.domain.dto.Entity;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.dto.PaymentDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class EntityFactoryImplTest {

  private final EntityFactoryImpl factory = new EntityFactoryImpl();

  static Stream<Arguments> bindings() {
    return Stream.of(
        Arguments.of(Address.class, AddressDto.class, AddressImpl.class),
        Arguments.of(Country.class, CountryDto.class, CountryImpl.class),
        Arguments.of(DenialReason.class, DenialReasonDto.class, DenialReasonImpl.class),
        Arguments.of(Document.class, DocumentDto.class, DocumentImpl.class),
        Arguments.of(Mobility.class, MobilityDto.class, MobilityImpl.class),
        Arguments.of(MobilityChoice.class, MobilityChoiceDto.class, MobilityChoiceImpl.class),
        Arguments.of(NominatedStudent.class, NominatedStudentDto.class,
            NominatedStudentImpl.class),
        Arguments.of(Option.class, OptionDto.class, OptionImpl.class),
        Arguments.of(Partner.class, PartnerDto.class, PartnerImpl.class),
        Arguments.of(PartnerOption.class, PartnerOptionDto.class, PartnerOptionImpl.class),
        Arguments.of(Payment.class, PaymentDto.class, PaymentImpl.class),
        Arguments.of(Programme.class, ProgrammeDto.class, ProgrammeImpl.class),
        Arguments.of(User.class, UserDto.class, UserImpl.class));
  }

  @ParameterizedTest(name = "{0} / {1} -> {2}")
  @MethodSource("bindings")
  void theBusinessAndDtoInterfacesBuildFreshInstancesOfTheSameImplementation(Class<?> business,
      Class<?> dto, Class<?> implementation) {
    Object fromBusiness = factory.build(business);
    Object fromDto = factory.build(dto);

    assertThat(fromBusiness).isExactlyInstanceOf(implementation).isInstanceOf(business);
    assertThat(fromDto).isExactlyInstanceOf(implementation).isInstanceOf(dto)
        .isNotSameAs(fromBusiness);
    assertThat(factory.getImplementations()).containsEntry(business, implementation)
        .containsEntry(dto, implementation);
  }

  @Test
  void everyBindingIsListedOnce() {
    assertThat(factory.getImplementations()).hasSize(26);
    assertThat(bindings()).hasSize(13);
  }

  @Test
  void theImplementationsMapIsReadOnly() {
    Map<Class<?>, Class<?>> implementations = factory.getImplementations();

    assertThatThrownBy(() -> implementations.put(String.class, String.class))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedTest
  @ValueSource(classes = {String.class, Entity.class, AddressImpl.class})
  void buildingAnUnboundClassFails(Class<?> unbound) {
    assertThatThrownBy(() -> factory.build(unbound)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(unbound.getName());
  }
}
