package com.dragomitch.ipl.pae.adapter.out.persistence;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityChoiceEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.stereotype.Repository;

/** The mobility choices the students submit, in their order of preference. */
@Repository
class MobilityChoicePersistenceAdapter implements MobilityChoiceDao {

  /**
   * A choice with everything its DTO carries, in one query: its student and the student's option
   * (both required), its programme, and its country, denial reason and partner when it has them.
   */
  private static final String SELECT = "SELECT c FROM MobilityChoiceEntity c"
      + " JOIN FETCH c.user u JOIN FETCH u.option JOIN FETCH c.programme"
      + " LEFT JOIN FETCH c.country LEFT JOIN FETCH c.denialReason LEFT JOIN FETCH c.partner";

  private static final String ORDER = " ORDER BY c.id";

  /** The choice is still pending: neither denied nor cancelled. */
  private static final String PENDING = "c.denialReason IS NULL AND c.cancellationReason IS NULL";

  /** The choice did not become a mobility. */
  private static final String NOT_A_MOBILITY =
      "NOT EXISTS (SELECT 1 FROM MobilityEntity m WHERE m.id = c.id)";

  /** The choice did not become a mobility that was then denied or cancelled. */
  private static final String NOT_A_CANCELLED_MOBILITY =
      "NOT EXISTS (SELECT 1 FROM MobilityEntity m WHERE m.id = c.id"
      + " AND (m.denialReason IS NOT NULL OR m.cancellationReason IS NOT NULL))";

  private final PersistenceMapper mapper;

  @PersistenceContext
  private EntityManager entityManager;

  MobilityChoicePersistenceAdapter(PersistenceMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Stores the choice at version 1, submitted now (the database's time), and sets its generated id,
   * submission date and version on the DTO.
   */
  @Override
  public MobilityChoiceDto create(MobilityChoiceDto mobilityChoice) {
    MobilityChoiceEntity entity = new MobilityChoiceEntity();
    entity.setUser(entityManager.getReference(UserEntity.class, mobilityChoice.getUser().getId()));
    copy(mobilityChoice, entity);
    // a partner id of -1 is how the form says "no partner"
    if (mobilityChoice.getPartner() != null && mobilityChoice.getPartner().getId() == -1) {
      entity.setPartner(null);
    }
    entity.setSubmissionDate(
        entityManager.createQuery("SELECT LOCAL DATETIME", LocalDateTime.class).getSingleResult());
    entity.setVersion(1);
    entityManager.persist(entity);
    mobilityChoice.setId(entity.getId());
    mobilityChoice.setSubmissionDate(entity.getSubmissionDate());
    mobilityChoice.setVersion(entity.getVersion());
    return mobilityChoice;
  }

  /** The choice, whether it became a mobility or not. */
  @Override
  public MobilityChoiceDto findById(int mobilityChoiceId) {
    List<MobilityChoiceEntity> found = where("c.id = :id")
        .setParameter("id", mobilityChoiceId).getResultList();
    return found.isEmpty() ? null : choice(found.get(0));
  }

  /** The choices of the student that did not become mobilities. */
  @Override
  public List<MobilityChoiceDto> findByUser(int userId) {
    return choices(where("u.id = :user AND " + NOT_A_MOBILITY).setParameter("user", userId));
  }

  /** The choices of the partner that did not become mobilities. */
  @Override
  public List<MobilityChoiceDto> findByPartner(int partnerId) {
    return choices(where("c.partner.id = :partner AND " + NOT_A_MOBILITY)
        .setParameter("partner", partnerId));
  }

  @Override
  public List<MobilityChoiceDto> findByActivePartner(int partnerId) {
    return choices(where("c.partner.id = :partner AND c.academicYear = :year AND " + PENDING
        + " AND " + NOT_A_CANCELLED_MOBILITY)
        .setParameter("partner", partnerId)
        .setParameter("year", LocalDate.now().getYear()));
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the filter is not one of the {@code FILTER_*} constants
   *         (a programming error: the API rejects an unknown filter before, with a 400
   *         {@code VALIDATION_FAILED} problem)
   */
  @Override
  public List<MobilityChoiceDto> findAll(String filter) {
    int year = LocalDate.now().getYear();
    if (FILTER_ALL_MOBILITIES_CHOICES.equals(filter)) {
      return choices(entityManager.createQuery(SELECT + ORDER, MobilityChoiceEntity.class));
    } else if (FILTER_CANCELED_MOBILITIES_CHOICES.equals(filter)) {
      return choices(where("c.cancellationReason IS NOT NULL"));
    } else if (FILTER_REJECTED_MOBILITIES_CHOICES.equals(filter)) {
      return choices(where("c.denialReason IS NOT NULL"));
    } else if (FILTER_PASSED_MOBILITIES_CHOICES.equals(filter)) {
      return choices(where("c.academicYear < :year").setParameter("year", year));
    } else if (FILTER_ACTIVE_MOBILITIES_CHOICES.equals(filter)) {
      return choices(where("c.academicYear = :year AND " + PENDING + " AND " + NOT_A_MOBILITY)
          .setParameter("year", year));
    }
    throw new IllegalArgumentException("Unknown mobility choice filter: " + filter);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The student is not changed.
   */
  @Override
  public void update(MobilityChoiceDto mobilityChoice) {
    MobilityChoiceEntity entity = entityManager.find(MobilityChoiceEntity.class,
        mobilityChoice.getId());
    if (entity == null || entity.getVersion() != mobilityChoice.getVersion()) {
      throw new ConcurrentModificationException("The data have been modified before that query");
    }
    copy(mobilityChoice, entity);
    entity.setSubmissionDate(mobilityChoice.getSubmissionDate());
    Versions.flushIncrementingVersion(entityManager, entity, entity::getVersion,
        mobilityChoice.getVersion());
    mobilityChoice.setVersion(entity.getVersion());
  }

  /** The {@link #SELECT} restricted by the condition, by id. */
  private TypedQuery<MobilityChoiceEntity> where(String condition) {
    return entityManager.createQuery(SELECT + " WHERE " + condition + ORDER,
        MobilityChoiceEntity.class);
  }

  private List<MobilityChoiceDto> choices(TypedQuery<MobilityChoiceEntity> query) {
    return query.getResultList().stream().map(this::choice).toList();
  }

  /**
   * The columns a create and an update write. An empty cancellation reason is stored as NULL (see
   * {@link MobilityChoiceDao#update}).
   */
  private void copy(MobilityChoiceDto choice, MobilityChoiceEntity entity) {
    entity.setPreferenceOrder(choice.getPreferenceOrder());
    entity.setMobilityType(choice.getMobilityType());
    entity.setAcademicYear(choice.getAcademicYear());
    entity.setTerm(choice.getTerm());
    entity.setProgramme(
        entityManager.getReference(ProgrammeEntity.class, choice.getProgramme().getId()));
    entity.setCountry(choice.getCountry() == null ? null
        : entityManager.getReference(CountryEntity.class, choice.getCountry().getCountryCode()));
    entity.setDenialReason(choice.getDenialReason() == null ? null
        : entityManager.getReference(DenialReasonEntity.class, choice.getDenialReason().getId()));
    entity.setCancellationReason(
        isAValidString(choice.getCancellationReason()) ? choice.getCancellationReason() : null);
    entity.setPartner(choice.getPartner() == null ? null
        : entityManager.getReference(PartnerEntity.class, choice.getPartner().getId()));
  }

  /**
   * A choice of {@link #SELECT}: its student carries their id, names and option (code and name),
   * its programme its id and name, its country its code and name, its partner its id and full
   * name.
   */
  private MobilityChoiceDto choice(MobilityChoiceEntity entity) {
    MobilityChoiceDto choice = mapper.build(MobilityChoiceDto.class);
    choice.setId(entity.getId());
    UserEntity userEntity = entity.getUser();
    UserDto user = mapper.build(UserDto.class);
    user.setId(userEntity.getId());
    user.setLastName(userEntity.getLastName());
    user.setFirstName(userEntity.getFirstName());
    user.setOption(mapper.option(userEntity.getOption()));
    choice.setUser(user);
    choice.setPreferenceOrder(entity.getPreferenceOrder());
    choice.setMobilityType(entity.getMobilityType());
    choice.setAcademicYear(entity.getAcademicYear());
    choice.setTerm(entity.getTerm());
    ProgrammeEntity programmeEntity = entity.getProgramme();
    ProgrammeDto programme = mapper.programmeReference(programmeEntity.getId());
    programme.setProgrammeName(programmeEntity.getName());
    choice.setProgramme(programme);
    choice.setCountry(entity.getCountry() == null ? null
        : mapper.countryNamed(entity.getCountry()));
    choice.setSubmissionDate(entity.getSubmissionDate());
    choice.setDenialReason(entity.getDenialReason() == null ? null
        : mapper.denialReason(entity.getDenialReason()));
    choice.setCancellationReason(entity.getCancellationReason());
    PartnerEntity partnerEntity = entity.getPartner();
    if (partnerEntity != null) {
      PartnerDto partner = mapper.build(PartnerDto.class);
      partner.setId(partnerEntity.getId());
      partner.setFullName(partnerEntity.getFullName());
      choice.setPartner(partner);
    }
    choice.setVersion(entity.getVersion());
    return choice;
  }
}
