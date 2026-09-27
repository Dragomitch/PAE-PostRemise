package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.Country;
import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.Document;
import com.dragomitch.ipl.pae.business.EntityFactory;
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
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * Builds business objects from their business or DTO interface. Replaces the former reflection
 * lookup in the properties files: the bindings are checked by the compiler.
 */
@Component
class EntityFactoryImpl implements EntityFactory {

  private final Map<Class<?>, Supplier<?>> suppliers = new LinkedHashMap<>();
  private final Map<Class<?>, Class<?>> implementations = new LinkedHashMap<>();

  EntityFactoryImpl() {
    bind(AddressImpl.class, AddressImpl::new, Address.class, AddressDto.class);
    bind(CountryImpl.class, CountryImpl::new, Country.class, CountryDto.class);
    bind(DenialReasonImpl.class, DenialReasonImpl::new, DenialReason.class, DenialReasonDto.class);
    bind(DocumentImpl.class, DocumentImpl::new, Document.class, DocumentDto.class);
    bind(MobilityImpl.class, MobilityImpl::new, Mobility.class, MobilityDto.class);
    bind(MobilityChoiceImpl.class, MobilityChoiceImpl::new, MobilityChoice.class,
        MobilityChoiceDto.class);
    bind(NominatedStudentImpl.class, NominatedStudentImpl::new, NominatedStudent.class,
        NominatedStudentDto.class);
    bind(OptionImpl.class, OptionImpl::new, Option.class, OptionDto.class);
    bind(PartnerImpl.class, PartnerImpl::new, Partner.class, PartnerDto.class);
    bind(PartnerOptionImpl.class, PartnerOptionImpl::new, PartnerOption.class,
        PartnerOptionDto.class);
    bind(PaymentImpl.class, PaymentImpl::new, Payment.class, PaymentDto.class);
    bind(ProgrammeImpl.class, ProgrammeImpl::new, Programme.class, ProgrammeDto.class);
    bind(UserImpl.class, UserImpl::new, User.class, UserDto.class);
  }

  private <T> void bind(Class<T> implementation, Supplier<T> supplier, Class<?>... interfaces) {
    for (Class<?> iface : interfaces) {
      suppliers.put(iface, supplier);
      implementations.put(iface, implementation);
    }
  }

  /**
   * Returns a new instance of the implementation bound to the Class cl.
   * 
   * @param cl the Class that the caller needs an instance of
   * @return an instance of the given class
   */
  @Override
  public Object build(Class<?> cl) {
    Supplier<?> supplier = suppliers.get(cl);
    if (supplier == null) {
      throw new IllegalArgumentException("No implementation bound to " + cl.getName());
    }
    return supplier.get();
  }

  @Override
  public Map<Class<?>, Class<?>> getImplementations() {
    return Collections.unmodifiableMap(implementations);
  }

}
