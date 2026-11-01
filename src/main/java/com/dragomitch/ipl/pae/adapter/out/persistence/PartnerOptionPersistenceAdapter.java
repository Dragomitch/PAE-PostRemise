package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.OptionEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerOptionEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.PartnerOptionId;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.PartnerOptionRepository;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

/** The options each partner welcomes. */
@Repository
class PartnerOptionPersistenceAdapter implements PartnerOptionDao {

  private final PartnerOptionRepository partnerOptions;
  private final PersistenceMapper mapper;

  @PersistenceContext
  private EntityManager entityManager;

  PartnerOptionPersistenceAdapter(PartnerOptionRepository partnerOptions,
      PersistenceMapper mapper) {
    this.partnerOptions = partnerOptions;
    this.mapper = mapper;
  }

  /** Stores the option of the partner, and writes it at once (its key is not generated). */
  @Override
  public PartnerOptionDto create(PartnerOptionDto partnerOption, int partnerId) {
    PartnerOptionEntity entity = new PartnerOptionEntity();
    entity.setId(new PartnerOptionId(partnerOption.getCode(), partnerId));
    entity.setOption(entityManager.getReference(OptionEntity.class, partnerOption.getCode()));
    PartnerEntity partner = entityManager.getReference(PartnerEntity.class, partnerId);
    entity.setPartner(partner);
    entity.setDepartment(partnerOption.getDepartement());
    entityManager.persist(entity);
    // keep both sides of the association in sync when the partner is already loaded
    if (Hibernate.isInitialized(partner)) {
      partner.getOptions().add(entity);
    }
    entityManager.flush();
    return partnerOption;
  }

  /** The partners offering the option, each known by its id only. */
  @Override
  public List<PartnerDto> findAllPartnersByOption(String optionCode) {
    return partnerOptions.findPartnerIdsByOption(optionCode).stream().map(id -> {
      PartnerDto partner = mapper.build(PartnerDto.class);
      partner.setId(id);
      return partner;
    }).toList();
  }

  /** The options of the partner: code, department and name. */
  @Override
  public List<PartnerOptionDto> findAllOptionsByPartner(int partnerId) {
    return partnerOptions.findByPartnerWithOption(partnerId).stream().map(entity -> {
      PartnerOptionDto option = mapper.build(PartnerOptionDto.class);
      option.setCode(entity.getId().getOptionCode());
      option.setDepartement(entity.getDepartment());
      option.setName(entity.getOption().getName());
      return option;
    }).toList();
  }
}
