package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InMemoryMobilityRepository implements MobilityRepository, ResettableRepository {

  private MobilityChoiceRepository mobilityChoiceRepository;
  private EntityFactory entityFactory;
  private Set<MobilityDto> mobilities = new HashSet<MobilityDto>();

  public InMemoryMobilityRepository(EntityFactory entityFactory, MobilityChoiceRepository mobilityChoiceRepository) {
    this.entityFactory = entityFactory;
    this.mobilityChoiceRepository = mobilityChoiceRepository;
  }

  @Override
  public MobilityDto create(MobilityDto mobility) {
    mobilities.add(mobility);
    mobility.setVersion(1);
    return mobility;
  }

  @Override
  public List<MobilityDto> findAll() {
    List<MobilityDto> mobilities = new ArrayList<MobilityDto>();
    for (MobilityDto curMobility : this.mobilities) {
      mobilities.add(curMobility);
    }
    return mobilities;
  }

  @Override
  public MobilityDto findById(int id) {
    for (MobilityDto curMobility : this.mobilities) {
      if (curMobility.getId() == id) {
        MobilityChoiceDto mobilityChoice = mobilityChoiceRepository.findById(id);
        NominatedStudent student = (NominatedStudent) entityFactory.build(NominatedStudent.class);
        student.setId(mobilityChoice.getUser().getId());
        curMobility.setNominatedStudent(student);
        return curMobility;
      }
    }
    return null;
  }

  @Override
  public List<MobilityDto> findByUser(int user) {
    List<MobilityDto> mobilities = new ArrayList<MobilityDto>();
    for (MobilityDto curMobility : this.mobilities) {
      if (curMobility.getUser().getId() == user) {
        mobilities.add(curMobility);
      }
    }
    return mobilities;
  }

  @Override
  public MobilityDto update(MobilityDto mobility) {
    mobility.setVersion(mobility.getVersion() + 1);
    return mobility;
  }

  @Override
  public void empty() {
    mobilities = new HashSet<MobilityDto>();
  }
}
