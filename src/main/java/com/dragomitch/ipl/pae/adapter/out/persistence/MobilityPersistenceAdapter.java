package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityChoiceEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.MobilityJpaRepository;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.MobilityDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.stereotype.Repository;

/** The mobilities: the accepted mobility choices, followed until they are closed. */
@Repository
class MobilityPersistenceAdapter implements MobilityDao {

  private final MobilityJpaRepository mobilities;
  private final PersistenceMapper mapper;

  @PersistenceContext
  private EntityManager entityManager;

  MobilityPersistenceAdapter(MobilityJpaRepository mobilities, PersistenceMapper mapper) {
    this.mobilities = mobilities;
    this.mapper = mapper;
  }

  /**
   * Stores the mobility of the choice (the DTO's id) at version 1, and writes it at once (its key
   * is not generated).
   */
  @Override
  public MobilityDto create(MobilityDto mobility) {
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
  public MobilityDto findById(int id) {
    return mobilities.findWithChoiceById(id).map(this::mobility).orElse(null);
  }

  @Override
  public List<MobilityDto> findAll() {
    return mobilities.findAllWithChoiceByOrderById().stream().map(this::mobility).toList();
  }

  @Override
  public List<MobilityDto> findByUser(int user) {
    return mobilities.findWithChoiceByChoiceUserIdOrderById(user).stream().map(this::mobility)
        .toList();
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the mobility does not exist or its version is not
   *         the DTO's (it was changed since it was read)
   */
  @Override
  public MobilityDto update(MobilityDto mobility) {
    MobilityEntity entity = mobilities.findById(mobility.getId())
        .filter(found -> found.getVersion() == mobility.getVersion())
        .orElseThrow(ConcurrentModificationException::new);
    copy(mobility, entity);
    Versions.flushIncrementingVersion(entityManager, entity, entity::getVersion,
        mobility.getVersion());
    mobility.setVersion(entity.getVersion());
    return mobility;
  }

  /** The columns a create and an update write. */
  private void copy(MobilityDto mobility, MobilityEntity entity) {
    entity.setSubmissionDate(mobility.getSubmissionDate());
    entity.setState(mobility.getState());
    entity.setStateBeforeCancellation(mobility.getStateBeforeCancellation());
    entity.setFirstPaymentRequestDate(mobility.getFirstPaymentRequestDate());
    entity.setSecondPaymentRequestDate(mobility.getSecondPaymentRequestDate());
    entity.setProEcoEncoding(mobility.isEncodedInProEco());
    entity.setSecondSoftwareEncoding(mobility.isEncodedInSecondSoftware());
    entity.setCancellationReason(mobility.getCancellationReason());
    entity.setDenialReason(mobility.getDenialReason() == null ? null
        : entityManager.getReference(DenialReasonEntity.class,
            mobility.getDenialReason().getId()));
    entity.setProfessorInCharge(mobility.getProfessorInCharge() == null ? null
        : entityManager.getReference(UserEntity.class, mobility.getProfessorInCharge().getId()));
  }

  /**
   * A mobility with its choice's type, year and term; its student (id, names and option code);
   * its partner (id and full name) and country (code and name), when the choice has them; its
   * programme (id and name); its denial reason; and its professor in charge (id only).
   */
  private MobilityDto mobility(MobilityEntity entity) {
    MobilityChoiceEntity choice = entity.getChoice();
    MobilityDto mobility = mapper.build(MobilityDto.class);
    mobility.setId(entity.getId());
    mobility.setMobilityType(choice.getMobilityType());
    mobility.setAcademicYear(choice.getAcademicYear());
    mobility.setTerm(choice.getTerm());
    mobility.setSubmissionDate(entity.getSubmissionDate());
    mobility.setState(entity.getState());
    mobility.setStateBeforeCancellation(entity.getStateBeforeCancellation());
    mobility.setFirstPaymentRequestDate(entity.getFirstPaymentRequestDate());
    mobility.setSecondPaymentRequestDate(entity.getSecondPaymentRequestDate());
    mobility.setProEcoEncoding(Boolean.TRUE.equals(entity.getProEcoEncoding()));
    mobility.setSecondSoftwareEncoding(Boolean.TRUE.equals(entity.getSecondSoftwareEncoding()));
    mobility.setCancellationReason(entity.getCancellationReason());
    if (entity.getDenialReason() != null) {
      mobility.setDenialReason(mapper.denialReason(entity.getDenialReason()));
    }
    if (entity.getProfessorInCharge() != null) {
      UserDto professor = mapper.build(UserDto.class);
      professor.setId(entity.getProfessorInCharge().getId());
      mobility.setProfessorInCharge(professor);
    }
    mobility.setVersion(entity.getVersion());
    UserEntity user = choice.getUser();
    NominatedStudentDto student = mapper.build(NominatedStudentDto.class);
    student.setId(user.getId());
    student.setFirstName(user.getFirstName());
    student.setLastName(user.getLastName());
    OptionDto option = mapper.build(OptionDto.class);
    option.setCode(user.getOption().getCode());
    student.setOption(option);
    mobility.setNominatedStudent(student);
    PartnerEntity partnerEntity = choice.getPartner();
    if (partnerEntity != null) {
      PartnerDto partner = mapper.build(PartnerDto.class);
      partner.setId(partnerEntity.getId());
      partner.setFullName(partnerEntity.getFullName());
      mobility.setPartner(partner);
    }
    if (choice.getCountry() != null) {
      mobility.setCountry(mapper.countryNamed(choice.getCountry()));
    }
    ProgrammeEntity programmeEntity = choice.getProgramme();
    ProgrammeDto programme = mapper.programmeReference(programmeEntity.getId());
    programme.setProgrammeName(programmeEntity.getName());
    mobility.setProgramme(programme);
    return mobility;
  }
}
