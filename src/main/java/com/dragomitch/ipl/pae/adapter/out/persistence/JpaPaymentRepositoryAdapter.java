package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityChoiceEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.MobilityJpaRepository;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Payment;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.PaymentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** The payments requested for the mobilities: a read model over the mobilities. */
@Repository
class JpaPaymentRepositoryAdapter implements PaymentRepository {

  /** The type of the payment of the first request. */
  private static final String FIRST_PAYMENT = "D";

  /** The type of the payment of the second request. */
  private static final String SECOND_PAYMENT = "R";

  private final MobilityJpaRepository mobilities;
  private final PersistenceMapper mapper;

  JpaPaymentRepositoryAdapter(MobilityJpaRepository mobilities, PersistenceMapper mapper) {
    this.mobilities = mobilities;
    this.mapper = mapper;
  }

  /**
   * One payment per request date of a mobility: {@code "D"} for the first request, {@code "R"} for
   * the second one, by mobility.
   */
  @Override
  public List<Payment> findAll() {
    List<Payment> payments = new ArrayList<>();
    for (MobilityEntity mobility : mobilities.findWithPaymentRequests()) {
      if (mobility.getFirstPaymentRequestDate() != null) {
        payments.add(payment(mobility, mobility.getFirstPaymentRequestDate(), FIRST_PAYMENT));
      }
      if (mobility.getSecondPaymentRequestDate() != null) {
        payments.add(payment(mobility, mobility.getSecondPaymentRequestDate(), SECOND_PAYMENT));
      }
    }
    return payments;
  }

  /**
   * A payment of the mobility: its choice's student (id and names), type, year, term and programme
   * (id and name), and its partner (id and full name) and country (code and name) when the choice
   * has them.
   */
  private Payment payment(MobilityEntity mobility, LocalDateTime date, String type) {
    MobilityChoiceEntity choice = mobility.getChoice();
    Payment payment = new Payment();
    payment.setMobilityChoiceId(mobility.getId());
    UserEntity userEntity = choice.getUser();
    User user = new User();
    user.setId(userEntity.getId());
    user.setFirstName(userEntity.getFirstName());
    user.setLastName(userEntity.getLastName());
    payment.setUser(user);
    payment.setMobilityType(MobilityType.valueOf(choice.getMobilityType()));
    payment.setAcademicYear(String.valueOf(choice.getAcademicYear()));
    payment.setTerm(choice.getTerm());
    ProgrammeEntity programmeEntity = choice.getProgramme();
    Programme programme = mapper.programmeReference(programmeEntity.getId());
    programme.setProgrammeName(programmeEntity.getName());
    payment.setProgramme(programme);
    if (choice.getCountry() != null) {
      payment.setCountry(mapper.countryNamed(choice.getCountry()));
    }
    PartnerEntity partnerEntity = choice.getPartner();
    if (partnerEntity != null) {
      Partner partner = new Partner();
      partner.setId(partnerEntity.getId());
      partner.setFullName(partnerEntity.getFullName());
      payment.setPartner(partner);
    }
    payment.setPaymentDate(date);
    payment.setPaymentType(type);
    return payment;
  }
}
