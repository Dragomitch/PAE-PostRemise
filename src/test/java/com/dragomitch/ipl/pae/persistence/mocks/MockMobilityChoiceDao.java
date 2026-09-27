package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MockMobilityChoiceDao implements MobilityChoiceDao {

  private List<MobilityChoiceDto> mobilityChoices;

  public MockMobilityChoiceDao() {
    mobilityChoices = new ArrayList<MobilityChoiceDto>();
  }

  @Override
  public MobilityChoiceDto create(MobilityChoiceDto mobilityChoice) {
    mobilityChoice.setId(mobilityChoices.size() + 1);
    mobilityChoices.add(mobilityChoice);
    return mobilityChoice;
  }

  @Override
  public List<MobilityChoiceDto> findAll(String filter) {
    return mobilityChoices;
  }

  @Override
  public MobilityChoiceDto findById(int mobilityChoiceId) {
    if (mobilityChoiceId > 0 && mobilityChoiceId <= mobilityChoices.size()) {
      return mobilityChoices.get((mobilityChoiceId - 1));
    }
    return null;
  }

  @Override
  public void update(MobilityChoiceDto mobilityChoice) {
    mobilityChoices.set(mobilityChoice.getId() - 1, mobilityChoice);
    mobilityChoice.setVersion(mobilityChoice.getVersion() + 1);
  }

  public void empty() {
    mobilityChoices = new ArrayList<MobilityChoiceDto>();
  }

  @Override
  public List<MobilityChoiceDto> findByUser(int userId) {
    List<MobilityChoiceDto> mobilityChoicesForUser = new ArrayList<MobilityChoiceDto>();
    for (MobilityChoiceDto mobilityChoices : this.mobilityChoices) {
      if (mobilityChoices.getUser().getId() == userId) {
        mobilityChoicesForUser.add(mobilityChoices);
      }
    }
    return mobilityChoicesForUser;
  }

  @Override
  public List<MobilityChoiceDto> findByPartner(int partnerId) {
    List<MobilityChoiceDto> mobilityChoicesForPartner = new ArrayList<MobilityChoiceDto>();
    for (MobilityChoiceDto mobilityChoices : this.mobilityChoices) {
      if (mobilityChoices.getPartner() != null
          && mobilityChoices.getPartner().getId() == partnerId) {
        mobilityChoicesForPartner.add(mobilityChoices);
      }
    }
    return mobilityChoicesForPartner;
  }

  @Override
  public List<MobilityChoiceDto> findByActivePartner(int partnerId) {
    List<MobilityChoiceDto> mobilityChoicesForPartner = new ArrayList<MobilityChoiceDto>();
    for (MobilityChoiceDto mobilityChoices : this.mobilityChoices) {
      if (mobilityChoices.getPartner() != null && mobilityChoices.getPartner().getId() == partnerId
          && mobilityChoices.getCancellationReason() == null
          && mobilityChoices.getDenialReason() == null
          && mobilityChoices.getAcademicYear() == LocalDate.now().getYear()) {
        mobilityChoicesForPartner.add(mobilityChoices);
      }
    }
    return null;
  }
}
