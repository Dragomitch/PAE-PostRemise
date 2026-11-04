package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.OptionJpaRepository;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** The study options of the school (reference data). */
@Repository
class JpaOptionRepositoryAdapter implements OptionRepository {

  private final OptionJpaRepository options;
  private final PersistenceMapper mapper;

  JpaOptionRepositoryAdapter(OptionJpaRepository options, PersistenceMapper mapper) {
    this.options = options;
    this.mapper = mapper;
  }

  @Override
  public Optional<Option> findByCode(String code) {
    return options.findById(code).map(mapper::option);
  }

  /** In the order of {@code init.sql}. */
  @Override
  public List<Option> findAll() {
    return options.findAll().stream().map(mapper::option).toList();
  }
}
