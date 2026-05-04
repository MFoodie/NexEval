package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "practice_paper")
public class PracticePaper {

  @Id
  @Column(name = "id", nullable = false, length = 32)
  private String id;

  @Column(name = "name", nullable = false, length = 64)
  private String name;

  @Column(name = "course_no", length = 8)
  private String courseNo;

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

  public String getCourseNo() {
    return courseNo;
  }

  public void setCourseNo(String courseNo) {
    this.courseNo = courseNo;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
