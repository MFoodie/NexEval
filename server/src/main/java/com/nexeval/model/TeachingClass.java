package com.nexeval.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "class")
public class TeachingClass {

  @EmbeddedId
  private TeachingClassId id;

  public TeachingClassId getId() {
    return id;
  }

  public void setId(TeachingClassId id) {
    this.id = id;
  }
}