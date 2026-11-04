package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class InMemoryMobilityRepository implements MobilityRepository, ResettableRepository {

  private MobilityChoiceRepository mobilityChoiceRepository;
  private Set<Mobility> mobilities = new HashSet<Mobility>();

  public InMemoryMobilityRepository(MobilityChoiceRepository mobilityChoiceRepository) {
    this.mobilityChoiceRepository = mobilityChoiceRepository;
  }

  @Override
  public Mobility create(Mobility mobility) {
    mobilities.add(mobility);
    mobility.setVersion(1);
    return mobility;
  }

  @Override
  public List<Mobility> findAll() {
    List<Mobility> mobilities = new ArrayList<Mobility>();
    for (Mobility curMobility : this.mobilities) {
      mobilities.add(curMobility);
    }
    return mobilities;
  }

  @Override
  public Optional<Mobility> findById(int id) {
    for (Mobility curMobility : this.mobilities) {
      if (curMobility.getId() == id) {
        MobilityChoice mobilityChoice = mobilityChoiceRepository.findById(id).orElse(null);
        NominatedStudent student = new NominatedStudent();
        student.setId(mobilityChoice.getUser().getId());
        curMobility.setNominatedStudent(student);
        return Optional.ofNullable(curMobility);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Mobility> findByUser(int user) {
    List<Mobility> mobilities = new ArrayList<Mobility>();
    for (Mobility curMobility : this.mobilities) {
      if (curMobility.getUser().getId() == user) {
        mobilities.add(curMobility);
      }
    }
    return mobilities;
  }

  @Override
  public Mobility update(Mobility mobility) {
    mobility.setVersion(mobility.getVersion() + 1);
    return mobility;
  }

  @Override
  public void empty() {
    mobilities = new HashSet<Mobility>();
  }
}
