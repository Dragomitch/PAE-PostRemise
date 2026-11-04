package com.dragomitch.ipl.pae.adapter.out.persistence;

import static com.dragomitch.ipl.pae.domain.util.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityChoiceEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.StrictFindByIdRepository;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** The mobility choices the students submit, in their order of preference. */
@Repository
class JpaMobilityChoiceRepositoryAdapter implements MobilityChoiceRepository {

  /**
   * A choice with everything its DTO carries, in one query: its student and the student's option
   * (both required), its programme, and its country, denial reason and partner when it has them.
   */
  private static final String SELECT =
      "SELECT c FROM MobilityChoiceEntity c"
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
  private final Clock clock;

  @PersistenceContext private EntityManager entityManager;

  JpaMobilityChoiceRepositoryAdapter(PersistenceMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  /**
   * Stores the choice at version 1, submitted now (the database's time), and sets its generated id,
   * submission date and version on the DTO.
   */
  @Override
  public MobilityChoice create(MobilityChoice mobilityChoice) {
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
  public Optional<MobilityChoice> findById(int mobilityChoiceId) {
    List<MobilityChoiceEntity> found =
        where("c.id = :id").setParameter("id", mobilityChoiceId).getResultList();
    return found.stream().findFirst().map(this::choice);
  }

  /** The choices of the student that did not become mobilities. */
  @Override
  public List<MobilityChoice> findByUser(int userId) {
    return choices(where("u.id = :user AND " + NOT_A_MOBILITY).setParameter("user", userId));
  }

  /** The choices of the partner that did not become mobilities. */
  @Override
  public List<MobilityChoice> findByPartner(int partnerId) {
    return choices(
        where("c.partner.id = :partner AND " + NOT_A_MOBILITY).setParameter("partner", partnerId));
  }

  @Override
  public List<MobilityChoice> findByActivePartner(int partnerId) {
    return choices(
        where(
                "c.partner.id = :partner AND c.academicYear = :year AND "
                    + PENDING
                    + " AND "
                    + NOT_A_CANCELLED_MOBILITY)
            .setParameter("partner", partnerId)
            .setParameter("year", LocalDate.now(clock).getYear()));
  }

  @Override
  public List<MobilityChoice> findAll(MobilityChoiceFilter filter) {
    int year = LocalDate.now(clock).getYear();
    return switch (filter) {
      case ALL -> choices(entityManager.createQuery(SELECT + ORDER, MobilityChoiceEntity.class));
      case CANCELED -> choices(where("c.cancellationReason IS NOT NULL"));
      case REJECTED -> choices(where("c.denialReason IS NOT NULL"));
      case PASSED -> choices(where("c.academicYear < :year").setParameter("year", year));
      case ACTIVE ->
          choices(
              where("c.academicYear = :year AND " + PENDING + " AND " + NOT_A_MOBILITY)
                  .setParameter("year", year));
    };
  }

  /**
   * {@inheritDoc}
   *
   * <p>The student is not changed.
   */
  @Override
  public void update(MobilityChoice mobilityChoice) {
    MobilityChoiceEntity entity =
        StrictFindByIdRepository.find(
            entityManager, MobilityChoiceEntity.class, mobilityChoice.getId());
    if (entity == null || entity.getVersion() != mobilityChoice.getVersion()) {
      throw new ConcurrentModificationException("The data have been modified before that query");
    }
    copy(mobilityChoice, entity);
    entity.setSubmissionDate(mobilityChoice.getSubmissionDate());
    Versions.flushIncrementingVersion(
        entityManager, entity, entity::getVersion, mobilityChoice.getVersion());
    mobilityChoice.setVersion(entity.getVersion());
  }

  /** The {@link #SELECT} restricted by the condition, by id. */
  private TypedQuery<MobilityChoiceEntity> where(String condition) {
    return entityManager.createQuery(
        SELECT + " WHERE " + condition + ORDER, MobilityChoiceEntity.class);
  }

  private List<MobilityChoice> choices(TypedQuery<MobilityChoiceEntity> query) {
    return query.getResultList().stream().map(this::choice).toList();
  }

  /**
   * The columns a create and an update write. An empty cancellation reason is stored as NULL (see
   * {@link MobilityChoiceRepository#update}).
   */
  private void copy(MobilityChoice choice, MobilityChoiceEntity entity) {
    entity.setPreferenceOrder(choice.getPreferenceOrder());
    entity.setMobilityType(
        choice.getMobilityType() == null ? null : choice.getMobilityType().name());
    entity.setAcademicYear(choice.getAcademicYear());
    entity.setTerm(choice.getTerm());
    entity.setProgramme(
        entityManager.getReference(ProgrammeEntity.class, choice.getProgramme().getId()));
    entity.setCountry(
        choice.getCountry() == null
            ? null
            : entityManager.getReference(
                CountryEntity.class, choice.getCountry().getCountryCode()));
    entity.setDenialReason(
        choice.getDenialReason() == null
            ? null
            : entityManager.getReference(
                DenialReasonEntity.class, choice.getDenialReason().getId()));
    entity.setCancellationReason(
        isAValidString(choice.getCancellationReason()) ? choice.getCancellationReason() : null);
    entity.setPartner(
        choice.getPartner() == null
            ? null
            : entityManager.getReference(PartnerEntity.class, choice.getPartner().getId()));
  }

  /**
   * A choice of {@link #SELECT}: its student carries their id, names and option (code and name),
   * its programme its id and name, its country its code and name, its partner its id and full name.
   */
  private MobilityChoice choice(MobilityChoiceEntity entity) {
    MobilityChoice choice = new MobilityChoice();
    choice.setId(entity.getId());
    UserEntity userEntity = entity.getUser();
    User user = new User();
    user.setId(userEntity.getId());
    user.setLastName(userEntity.getLastName());
    user.setFirstName(userEntity.getFirstName());
    user.setOption(mapper.option(userEntity.getOption()));
    choice.setUser(user);
    choice.setPreferenceOrder(entity.getPreferenceOrder());
    choice.setMobilityType(MobilityType.valueOf(entity.getMobilityType()));
    choice.setAcademicYear(entity.getAcademicYear());
    choice.setTerm(entity.getTerm());
    ProgrammeEntity programmeEntity = entity.getProgramme();
    Programme programme = mapper.programmeReference(programmeEntity.getId());
    programme.setProgrammeName(programmeEntity.getName());
    choice.setProgramme(programme);
    choice.setCountry(
        entity.getCountry() == null ? null : mapper.countryNamed(entity.getCountry()));
    choice.setSubmissionDate(entity.getSubmissionDate());
    choice.setDenialReason(
        entity.getDenialReason() == null ? null : mapper.denialReason(entity.getDenialReason()));
    choice.setCancellationReason(entity.getCancellationReason());
    PartnerEntity partnerEntity = entity.getPartner();
    if (partnerEntity != null) {
      Partner partner = new Partner();
      partner.setId(partnerEntity.getId());
      partner.setFullName(partnerEntity.getFullName());
      choice.setPartner(partner);
    }
    choice.setVersion(entity.getVersion());
    return choice;
  }
}
