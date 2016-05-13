package persistence.mocks;

import business.dto.NominatedStudentDto;
import persistence.NominatedStudentDao;

import java.util.ArrayList;
import java.util.List;

public class MockNominatedStudentDao implements NominatedStudentDao {

  private List<NominatedStudentDto> nominatedStudents;

  public MockNominatedStudentDao() {
    nominatedStudents = new ArrayList<NominatedStudentDto>();
  }

  @Override
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent) {
    nominatedStudent.setId(nominatedStudents.size() + 1);
    nominatedStudents.add(nominatedStudent);
    return nominatedStudent;
  }

  @Override
  public NominatedStudentDto findById(int id) {
    if (id > 0 && id <= nominatedStudents.size()) {
      return nominatedStudents.get(id - 1);
    }
    return null;
  }

  @Override
  public List<NominatedStudentDto> findAll() {
    return nominatedStudents;
  }

  @Override
  public NominatedStudentDto update(NominatedStudentDto nominatedStudent) {
    nominatedStudent.setVersion((findById(nominatedStudent.getId()).getVersion()) + 1);
    nominatedStudents.set(nominatedStudent.getId() - 1, nominatedStudent);
    return nominatedStudent;
  }

  public void empty() {
    nominatedStudents = new ArrayList<NominatedStudentDto>();
  }

}
