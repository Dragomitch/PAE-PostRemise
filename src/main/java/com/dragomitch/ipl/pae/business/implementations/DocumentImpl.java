package com.dragomitch.ipl.pae.business.implementations;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import com.dragomitch.ipl.pae.business.Document;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.DocumentDao;

import java.io.Serializable;

@DaoClass(DocumentDao.class)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DocumentImpl implements Document, Serializable {

  private static final long serialVersionUID = 1614430064652187814L;

  private int id;
  private String name;
  private char category;
  private boolean isFilledIn;
  private ProgrammeDto programme;
  private int version;

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public char getCategory() {
    return category;
  }

  @Override
  public void setCategory(char category) {
    this.category = category;
  }

  @Override
  public boolean isFilledIn() {
    return isFilledIn;
  }

  @Override
  public void setFilledIn(boolean isFilledIn) {
    this.isFilledIn = isFilledIn;
  }

  @Override
  public ProgrammeDto getProgramme() {
    return programme;
  }

  @Override
  public void setProgramme(ProgrammeDto programme) {
    this.programme = programme;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

}
