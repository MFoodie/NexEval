package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_paper")
public class ExamPaper {

  @Id
  @Column(name = "id", nullable = false, length = 32)
  private String id;

  @Column(name = "name", nullable = false, length = 64)
  private String name;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
