package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher")
public class TeacherProfile {

  @Id
  @Column(name = "eid", nullable = false, length = 8)
  private String eid;

  @Column(name = "id", nullable = false, length = 9)
  private String id;

  @Column(name = "enteryear", nullable = false)
  private int enterYear;

  @Column(name = "title", nullable = false, length = 32)
  private String title;

  @Column(name = "department", nullable = false, length = 30)
  private String department;

  public String getEid() {
    return eid;
  }

  public void setEid(String eid) {
    this.eid = eid;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getEnterYear() {
    return enterYear;
  }

  public void setEnterYear(int enterYear) {
    this.enterYear = enterYear;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }
}
