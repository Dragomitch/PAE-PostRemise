package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.ProgrammeJpaRepository;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** The mobility programmes (reference data). */
@Repository
class JpaProgrammeRepositoryAdapter implements ProgrammeRepository {

  private final ProgrammeJpaRepository programmes;
  private final PersistenceMapper mapper;

  JpaProgrammeRepositoryAdapter(ProgrammeJpaRepository programmes, PersistenceMapper mapper) {
    this.programmes = programmes;
    this.mapper = mapper;
  }

  @Override
  public ProgrammeDto findById(int id) {
    return programmes.findById(id).map(mapper::programme).orElse(null);
  }

  @Override
  public List<ProgrammeDto> findAll() {
    return programmes.findAll(Sort.by("id")).stream().map(mapper::programme).toList();
  }
}
