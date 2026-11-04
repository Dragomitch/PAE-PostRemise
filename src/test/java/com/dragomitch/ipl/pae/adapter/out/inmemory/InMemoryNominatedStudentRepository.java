package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.exceptions.FatalException;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link NominatedStudentRepository} following the JDBC contract: a nominated student has
 * the id of its user (primary key), is stored with the version carried by the DTO, and is updated
 * with optimistic locking.
 */
public class InMemoryNominatedStudentRepository
    implements NominatedStudentRepository, ResettableRepository {

  private Map<Integer, NominatedStudent> nominatedStudents;
  // the stored versions, kept apart because the stored DTO is shared with the callers
  private Map<Integer, Integer> versions;

  public InMemoryNominatedStudentRepository() {
    empty();
  }

  @Override
  public NominatedStudent create(NominatedStudent nominatedStudent) {
    if (nominatedStudents.containsKey(nominatedStudent.getId())) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG);
    }
    nominatedStudents.put(nominatedStudent.getId(), nominatedStudent);
    versions.put(nominatedStudent.getId(), nominatedStudent.getVersion());
    return nominatedStudent;
  }

  @Override
  public Optional<NominatedStudent> findById(int id) {
    NominatedStudent student = nominatedStudents.get(id);
    if (student != null) {
      student.setVersion(versions.get(id));
    }
    return Optional.ofNullable(student);
  }

  @Override
  public List<NominatedStudent> findAll() {
    return new ArrayList<NominatedStudent>(nominatedStudents.values());
  }

  @Override
  public NominatedStudent update(NominatedStudent nominatedStudent) {
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
  public void empty() {
    nominatedStudents = new LinkedHashMap<Integer, NominatedStudent>();
    versions = new HashMap<Integer, Integer>();
  }
}
