package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityChoiceEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.MobilityJpaRepository;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** The mobilities: the accepted mobility choices, followed until they are closed. */
@Repository
class JpaMobilityRepositoryAdapter implements MobilityRepository {

  private final MobilityJpaRepository mobilities;
  private final PersistenceMapper mapper;

  @PersistenceContext private EntityManager entityManager;

  JpaMobilityRepositoryAdapter(MobilityJpaRepository mobilities, PersistenceMapper mapper) {
    this.mobilities = mobilities;
    this.mapper = mapper;
  }

  /**
   * Stores the mobility of the choice (the DTO's id) at version 1, and writes it at once (its key
   * is not generated).
   */
  @Override
  public Mobility create(Mobility mobility) {
    MobilityEntity entity = new MobilityEntity();
    entity.setChoice(entityManager.getReference(MobilityChoiceEntity.class, mobility.getId()));
    copy(mobility, entity);
    entity.setVersion(1);
    entityManager.persist(entity);
    entityManager.flush();
    mobility.setVersion(entity.getVersion());
    return mobility;
  }

  @Override
  public Optional<Mobility> findById(int id) {
    return mobilities.findWithChoiceById(id).map(this::mobility);
  }

  @Override
  public List<Mobility> findAll() {
    return mobilities.findAllWithChoiceByOrderById().stream().map(this::mobility).toList();
  }

  @Override
  public List<Mobility> findByUser(int user) {
    return mobilities.findWithChoiceByChoiceUserIdOrderById(user).stream()
        .map(this::mobility)
        .toList();
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the mobility does not exist or its version is not
   *     the DTO's (it was changed since it was read)
   */
  @Override
  public Mobility update(Mobility mobility) {
    MobilityEntity entity =
        mobilities
            .findById(mobility.getId())
            .filter(found -> found.getVersion() == mobility.getVersion())
            .orElseThrow(ConcurrentModificationException::new);
    copy(mobility, entity);
    Versions.flushIncrementingVersion(
        entityManager, entity, entity::getVersion, mobility.getVersion());
    mobility.setVersion(entity.getVersion());
    return mobility;
  }

  /** The columns a create and an update write. */
  private void copy(Mobility mobility, MobilityEntity entity) {
    entity.setSubmissionDate(mobility.getSubmissionDate());
    entity.setState(mobility.getState() == null ? null : mobility.getState().label());
    entity.setStateBeforeCancellation(
        mobility.getStateBeforeCancellation() == null
            ? null
            : mobility.getStateBeforeCancellation().label());
    entity.setFirstPaymentRequestDate(mobility.getFirstPaymentRequestDate());
    entity.setSecondPaymentRequestDate(mobility.getSecondPaymentRequestDate());
    entity.setProEcoEncoding(mobility.isEncodedInProEco());
    entity.setSecondSoftwareEncoding(mobility.isEncodedInSecondSoftware());
    entity.setCancellationReason(mobility.getCancellationReason());
    entity.setDenialReason(
        mobility.getDenialReason() == null
            ? null
            : entityManager.getReference(
                DenialReasonEntity.class, mobility.getDenialReason().getId()));
    entity.setProfessorInCharge(
        mobility.getProfessorInCharge() == null
            ? null
            : entityManager.getReference(
                UserEntity.class, mobility.getProfessorInCharge().getId()));
  }

  /**
   * A mobility with its choice's type, year and term; its student (id, names and option code); its
   * partner (id and full name) and country (code and name), when the choice has them; its programme
   * (id and name); its denial reason; and its professor in charge (id only).
   */
  private Mobility mobility(MobilityEntity entity) {
    MobilityChoiceEntity choice = entity.getChoice();
    Mobility mobility = new Mobility();
    mobility.setId(entity.getId());
    mobility.setMobilityType(MobilityType.valueOf(choice.getMobilityType()));
    mobility.setAcademicYear(choice.getAcademicYear());
    mobility.setTerm(choice.getTerm());
    mobility.setSubmissionDate(entity.getSubmissionDate());
    mobility.setState(MobilityState.fromLabel(entity.getState()));
    mobility.setStateBeforeCancellation(
        entity.getStateBeforeCancellation() == null
            ? null
            : MobilityState.fromLabel(entity.getStateBeforeCancellation()));
    mobility.setFirstPaymentRequestDate(entity.getFirstPaymentRequestDate());
    mobility.setSecondPaymentRequestDate(entity.getSecondPaymentRequestDate());
    mobility.setProEcoEncoding(Boolean.TRUE.equals(entity.getProEcoEncoding()));
    mobility.setSecondSoftwareEncoding(Boolean.TRUE.equals(entity.getSecondSoftwareEncoding()));
    mobility.setCancellationReason(entity.getCancellationReason());
    if (entity.getDenialReason() != null) {
      mobility.setDenialReason(mapper.denialReason(entity.getDenialReason()));
    }
    if (entity.getProfessorInCharge() != null) {
      User professor = new User();
      professor.setId(entity.getProfessorInCharge().getId());
      mobility.setProfessorInCharge(professor);
    }
    mobility.setVersion(entity.getVersion());
    UserEntity user = choice.getUser();
    NominatedStudent student = new NominatedStudent();
    student.setId(user.getId());
    student.setFirstName(user.getFirstName());
    student.setLastName(user.getLastName());
    Option option = new Option();
    option.setCode(user.getOption().getCode());
    student.setOption(option);
    mobility.setNominatedStudent(student);
    PartnerEntity partnerEntity = choice.getPartner();
    if (partnerEntity != null) {
      Partner partner = new Partner();
      partner.setId(partnerEntity.getId());
      partner.setFullName(partnerEntity.getFullName());
      mobility.setPartner(partner);
    }
    if (choice.getCountry() != null) {
      mobility.setCountry(mapper.countryNamed(choice.getCountry()));
    }
    ProgrammeEntity programmeEntity = choice.getProgramme();
    Programme programme = mapper.programmeReference(programmeEntity.getId());
    programme.setProgrammeName(programmeEntity.getName());
    mobility.setProgramme(programme);
    return mobility;
  }
}
