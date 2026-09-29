package com.dragomitch.ipl.pae.business.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.Country;
import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.Document;
import com.dragomitch.ipl.pae.business.Mobility;
import com.dragomitch.ipl.pae.business.MobilityChoice;
import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.Option;
import com.dragomitch.ipl.pae.business.Partner;
import com.dragomitch.ipl.pae.business.PartnerOption;
import com.dragomitch.ipl.pae.business.Payment;
import com.dragomitch.ipl.pae.business.Programme;
import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.Entity;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.DaoClass;

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

  @ParameterizedTest(name = "{2} is bound to its DAO")
  @MethodSource("bindings")
  void everyImplementationDeclaresItsDao(Class<?> business, Class<?> dto,
      Class<?> implementation) {
    // UnitOfWorkImpl uses @DaoClass to find the DAO that performs the deferred updates
    DaoClass dao = implementation.getAnnotation(DaoClass.class);

    assertThat(dao).isNotNull();
    assertThat(dao.value().getSimpleName())
        .isEqualTo(implementation.getSimpleName().replace("Impl", "Dao"));
  }
}
