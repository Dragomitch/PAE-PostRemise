package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.OptionRepository;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.persistence.OptionDao;

import java.util.List;

import org.springframework.stereotype.Repository;

/** The study options of the school (reference data). */
@Repository
class OptionPersistenceAdapter implements OptionDao {

  private final OptionRepository options;
  private final PersistenceMapper mapper;

  OptionPersistenceAdapter(OptionRepository options, PersistenceMapper mapper) {
    this.options = options;
    this.mapper = mapper;
  }

  @Override
  public OptionDto findByCode(String code) {
    return options.findById(code).map(mapper::option).orElse(null);
  }

  /** In the order of {@code init.sql}. */
  @Override
  public List<OptionDto> findAll() {
    return options.findAll().stream().map(mapper::option).toList();
  }
}
