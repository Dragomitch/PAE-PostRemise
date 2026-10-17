package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.OptionEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.OptionRepository;

import java.util.List;

import org.springframework.stereotype.Repository;

/** {@link OptionDao} on top of the Spring Data {@link OptionRepository}. */
@Repository
class OptionDaoImpl implements OptionDao {

  private final EntityFactory entityFactory;
  private final OptionRepository options;

  OptionDaoImpl(EntityFactory entityFactory, OptionRepository options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public OptionDto findByCode(String code) {
    return DataAccess.call(() -> options.findById(code).map(this::toDto).orElse(null));
  }

  @Override
  public List<OptionDto> findAll() {
    return DataAccess.call(() -> options.findAll().stream().map(this::toDto).toList());
  }

  private OptionDto toDto(OptionEntity entity) {
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(entity.code());
    option.setName(entity.name());
    return option;
  }
}
