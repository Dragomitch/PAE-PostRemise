package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link NominatedStudentDao} following the JDBC contract: a nominated student has the
 * id of its user (primary key), is stored with the version carried by the DTO, and is updated
 * with optimistic locking.
 */
public class MockNominatedStudentDao implements NominatedStudentDao, ResettableMock {

  private Map<Integer, NominatedStudentDto> nominatedStudents;
  // the stored versions, kept apart because the stored DTO is shared with the callers
  private Map<Integer, Integer> versions;

  public MockNominatedStudentDao() {
    empty();
  }

  @Override
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent) {
    if (nominatedStudents.containsKey(nominatedStudent.getId())) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG);
    }
    nominatedStudents.put(nominatedStudent.getId(), nominatedStudent);
    versions.put(nominatedStudent.getId(), nominatedStudent.getVersion());
    return nominatedStudent;
  }

  @Override
  public NominatedStudentDto findById(int id) {
    NominatedStudentDto student = nominatedStudents.get(id);
    if (student != null) {
      student.setVersion(versions.get(id));
    }
    return student;
  }

  @Override
  public List<NominatedStudentDto> findAll() {
    return new ArrayList<NominatedStudentDto>(nominatedStudents.values());
  }

  @Override
  public NominatedStudentDto update(NominatedStudentDto nominatedStudent) {
    Integer stored = versions.get(nominatedStudent.getId());
    if (stored == null || stored != nominatedStudent.getVersion()) {
      throw new ConcurrentModificationException();
    }
    nominatedStudent.setVersion(stored + 1);
    nominatedStudents.put(nominatedStudent.getId(), nominatedStudent);
    versions.put(nominatedStudent.getId(), stored + 1);
    return nominatedStudent;
  }

  @Override
  public void reset() {
    empty();
  }

  public void empty() {
    nominatedStudents = new LinkedHashMap<Integer, NominatedStudentDto>();
    versions = new HashMap<Integer, Integer>();
  }

}
