package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.ProgrammeRepository;

import java.util.List;

import org.springframework.stereotype.Repository;

/** {@link ProgrammeDao} on top of the Spring Data {@link ProgrammeRepository}. */
@Repository
class ProgrammeDaoImpl implements ProgrammeDao {

  private final EntityFactory entityFactory;
  private final ProgrammeRepository programmes;

  ProgrammeDaoImpl(EntityFactory entityFactory, ProgrammeRepository programmes) {
    this.entityFactory = entityFactory;
    this.programmes = programmes;
  }

  @Override
  public ProgrammeDto findById(int id) {
    return DataAccess.call(() -> programmes.findById(id).map(this::toDto).orElse(null));
  }

  @Override
  public List<ProgrammeDto> findAll() {
    return DataAccess.call(() -> programmes.findAll().stream().map(this::toDto).toList());
  }

  /**
   * Maps a programme to a DTO; also used by the country DAO, whose DTOs embed their programme.
   *
   * @param entityFactory the DTO factory
   * @param entity the programme
   * @return a new DTO with every field of the programme
   */
  static ProgrammeDto toDto(EntityFactory entityFactory, ProgrammeEntity entity) {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(entity.id());
    programme.setProgrammeName(entity.name());
    programme.setExternalSoftName(entity.externalSoftwareName());
    return programme;
  }

  private ProgrammeDto toDto(ProgrammeEntity entity) {
    return toDto(entityFactory, entity);
  }
}
