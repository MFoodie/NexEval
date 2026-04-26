package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "SC")
public class ScRecord {

  @EmbeddedId
  private ScRecordId id;

  @Column(name = "grade")
  private Integer grade;

  public ScRecordId getId() {
    return id;
  }

  public void setId(ScRecordId id) {
    this.id = id;
  }

  public Integer getGrade() {
    return grade;
  }

  public void setGrade(Integer grade) {
    this.grade = grade;
  }
}