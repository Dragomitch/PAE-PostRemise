package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryMobilityChoiceRepository
    implements MobilityChoiceRepository, ResettableRepository {

  private List<MobilityChoice> mobilityChoices;

  public InMemoryMobilityChoiceRepository() {
    mobilityChoices = new ArrayList<MobilityChoice>();
  }

  @Override
  public MobilityChoice create(MobilityChoice mobilityChoice) {
    mobilityChoice.setId(mobilityChoices.size() + 1);
    mobilityChoices.add(mobilityChoice);
    return mobilityChoice;
  }

  @Override
  public List<MobilityChoice> findAll(MobilityChoiceFilter filter) {
    return mobilityChoices;
  }

  @Override
  public Optional<MobilityChoice> findById(int mobilityChoiceId) {
    if (mobilityChoiceId > 0 && mobilityChoiceId <= mobilityChoices.size()) {
      return Optional.ofNullable(mobilityChoices.get((mobilityChoiceId - 1)));
    }
    return Optional.empty();
  }

  @Override
  public void update(MobilityChoice mobilityChoice) {
    mobilityChoices.set(mobilityChoice.getId() - 1, mobilityChoice);
    mobilityChoice.setVersion(mobilityChoice.getVersion() + 1);
  }

  @Override
  public void empty() {
    mobilityChoices = new ArrayList<MobilityChoice>();
  }

  @Override
  public List<MobilityChoice> findByUser(int userId) {
    List<MobilityChoice> mobilityChoicesForUser = new ArrayList<MobilityChoice>();
    for (MobilityChoice mobilityChoices : this.mobilityChoices) {
      if (mobilityChoices.getUser().getId() == userId) {
        mobilityChoicesForUser.add(mobilityChoices);
      }
    }
    return mobilityChoicesForUser;
  }

  @Override
  public List<MobilityChoice> findByPartner(int partnerId) {
    List<MobilityChoice> mobilityChoicesForPartner = new ArrayList<MobilityChoice>();
    for (MobilityChoice mobilityChoices : this.mobilityChoices) {
      if (mobilityChoices.getPartner() != null
          && mobilityChoices.getPartner().getId() == partnerId) {
        mobilityChoicesForPartner.add(mobilityChoices);
      }
    }
    return mobilityChoicesForPartner;
  }

  @Override
  public List<MobilityChoice> findByActivePartner(int partnerId) {
    List<MobilityChoice> mobilityChoicesForPartner = new ArrayList<MobilityChoice>();
    for (MobilityChoice mobilityChoices : this.mobilityChoices) {
      if (mobilityChoices.getPartner() != null
          && mobilityChoices.getPartner().getId() == partnerId
          && mobilityChoices.getCancellationReason() == null
          && mobilityChoices.getDenialReason() == null
          && mobilityChoices.getAcademicYear() == LocalDate.now().getYear()) {
        mobilityChoicesForPartner.add(mobilityChoices);
      }
    }
    return mobilityChoicesForPartner;
  }
}
