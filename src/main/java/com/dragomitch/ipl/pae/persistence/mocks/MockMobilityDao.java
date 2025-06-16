package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MockMobilityDao implements MobilityDao {

  private MobilityChoiceDao mobilityChoiceDao;
  private DtoFactory dtoFactory;
  private Set<MobilityDto> mobilities = new HashSet<MobilityDto>();

  public MockMobilityDao(DtoFactory dtoFactory, MobilityChoiceDao mobilityChoiceDao) {
    this.dtoFactory = dtoFactory;
    this.mobilityChoiceDao = mobilityChoiceDao;
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
        MobilityChoiceDto mobilityChoice = mobilityChoiceDao.findById(id);
        NominatedStudent student = (NominatedStudent) dtoFactory.create(NominatedStudent.class);
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

  public void empty() {
    mobilities = new HashSet<MobilityDto>();
  }
}
