package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.PartnerJpaRepository;
import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** The partners hosting the mobilities, with the options they welcome. */
@Repository
class JpaPartnerRepositoryAdapter implements PartnerRepository {

  /**
   * A partner with everything its DTO carries, in one query: its address, the address's country and
   * that country's programme, and its options with their names.
   */
  private static final String SELECT =
      "SELECT DISTINCT p FROM PartnerEntity p"
          + " JOIN FETCH p.address a JOIN FETCH a.country c JOIN FETCH c.programme"
          + " LEFT JOIN FETCH p.options po LEFT JOIN FETCH po.option";

  private static final String ORDER = " ORDER BY p.id";

  /** The partner offers the option of the student ({@code :option}). */
  private static final String OFFERS_OPTION =
      "EXISTS (SELECT 1 FROM PartnerOptionEntity f"
          + " WHERE f.partner = p AND f.id.optionCode = :option)";

  private final PartnerJpaRepository partners;
  private final PersistenceMapper mapper;

  @PersistenceContext private EntityManager entityManager;

  JpaPartnerRepositoryAdapter(PartnerJpaRepository partners, PersistenceMapper mapper) {
    this.partners = partners;
    this.mapper = mapper;
  }

  /**
   * Stores the partner, not archived, at version 1, and sets its generated id and version on the
   * DTO.
   */
  @Override
  public PartnerDto create(PartnerDto partner) {
    PartnerEntity entity = new PartnerEntity();
    copy(partner, entity);
    entity.setArchived(false);
    entity.setVersion(1);
    entityManager.persist(entity);
    partner.setId(entity.getId());
    partner.setVersion(entity.getVersion());
    return partner;
  }

  @Override
  public PartnerDto findById(int id) {
    List<PartnerEntity> found =
        entityManager
            .createQuery(SELECT + " WHERE p.id = :id", PartnerEntity.class)
            .setParameter("id", id)
            .getResultList();
    return found.isEmpty() ? null : partner(found.get(0));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Professors see every partner matching the filter. Students only see official, non-archived
   * partners offering their option ({@link PartnerFilter#ALL} and {@link PartnerFilter#COUNTRY}).
   */
  @Override
  public List<PartnerDto> findAll(
      PartnerFilter filter, String value, Role userRole, String option) {
    List<String> conditions = new ArrayList<>();
    Map<String, Object> parameters = new LinkedHashMap<>();
    switch (filter) {
      case COUNTRY -> {
        conditions.add("c.code = :country");
        parameters.put("country", value);
      }
      case ARCHIVED -> {
        conditions.add("p.archived = TRUE");
        conditions.add("LOWER(p.fullName) LIKE :name");
        parameters.put("name", "%" + value.toLowerCase() + "%");
      }
      case ALL -> {
        // no condition
      }
    }
    if (userRole == Role.STUDENT && filter != PartnerFilter.ARCHIVED) {
      conditions.add("p.archived = FALSE");
      conditions.add("p.official = TRUE");
      conditions.add(OFFERS_OPTION);
      parameters.put("option", option);
    }
    String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    TypedQuery<PartnerEntity> query =
        entityManager.createQuery(SELECT + where + ORDER, PartnerEntity.class);
    parameters.forEach(query::setParameter);
    return query.getResultList().stream().map(this::partner).toList();
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the partner does not exist or its version is not the
   *     DTO's (it was changed since it was read)
   */
  @Override
  public PartnerDto update(PartnerDto partner) {
    PartnerEntity entity =
        partners
            .findById(partner.getId())
            .filter(found -> found.getVersion() == partner.getVersion())
            .orElseThrow(ConcurrentModificationException::new);
    copy(partner, entity);
    entity.setArchived(partner.isArchived());
    Versions.flushIncrementingVersion(
        entityManager, entity, entity::getVersion, partner.getVersion());
    partner.setVersion(entity.getVersion());
    return partner;
  }

  /**
   * The columns a create and an update write. The website is optional (the UI's form says so) but
   * its column is {@code NOT NULL}: a missing website is stored as an empty one, as the UI sends an
   * empty field.
   */
  private void copy(PartnerDto partner, PartnerEntity entity) {
    entity.setLegalName(partner.getLegalName());
    entity.setBusinessName(partner.getBusinessName());
    entity.setFullName(partner.getFullName());
    entity.setOrganisationType(partner.getOrganisationType());
    entity.setEmployeeCount(partner.getEmployeeCount());
    entity.setAddress(
        entityManager.getReference(AddressEntity.class, partner.getAddress().getId()));
    entity.setEmail(partner.getEmail());
    entity.setWebsite(partner.getWebsite() == null ? "" : partner.getWebsite());
    entity.setPhoneNumber(partner.getPhoneNumber());
    entity.setOfficial(partner.isOfficial());
  }

  /**
   * A partner of {@link #SELECT}: its address carries its id and country (code and name), its
   * programme is its country's (id and name), and its options are sorted by code.
   */
  private PartnerDto partner(PartnerEntity entity) {
    PartnerDto partner = mapper.build(PartnerDto.class);
    partner.setId(entity.getId());
    partner.setLegalName(entity.getLegalName());
    partner.setBusinessName(entity.getBusinessName());
    partner.setFullName(entity.getFullName());
    partner.setOrganisationType(entity.getOrganisationType());
    partner.setEmployeeCount(entity.getEmployeeCount());
    AddressDto address = mapper.build(AddressDto.class);
    address.setId(entity.getAddress().getId());
    address.setCountry(mapper.countryNamed(entity.getAddress().getCountry()));
    partner.setAddress(address);
    partner.setEmail(entity.getEmail());
    partner.setWebsite(entity.getWebsite());
    partner.setPhoneNumber(entity.getPhoneNumber());
    partner.setOfficial(entity.isOfficial());
    partner.setArchived(entity.isArchived());
    partner.setVersion(entity.getVersion());
    ProgrammeEntity programme = entity.getAddress().getCountry().getProgramme();
    ProgrammeDto programmeDto = mapper.programmeReference(programme.getId());
    programmeDto.setProgrammeName(programme.getName());
    partner.setProgramme(programmeDto);
    partner.setOptions(
        new ArrayList<>(
            entity.getOptions().stream()
                .sorted(Comparator.comparing(option -> option.getId().getOptionCode()))
                .map(option -> {
                  PartnerOptionDto dto = mapper.build(PartnerOptionDto.class);
                  dto.setCode(option.getId().getOptionCode());
                  dto.setDepartement(option.getDepartment());
                  dto.setName(option.getOption().getName());
                  return dto;
                })
                .toList()));
    return partner;
  }
}
