package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class StudentProfile {

  @Id
  @Column(name = "sno", nullable = false, length = 8)
  private String sno;

  @Column(name = "id", nullable = false, length = 9)
  private String id;

  @Column(name = "enteryear", nullable = false)
  private int enterYear;

  @Column(name = "major", nullable = false, length = 20)
  private String major;

  @Column(name = "department", nullable = false, length = 30)
  private String department;

  public String getSno() {
    return sno;
  }

  public void setSno(String sno) {
    this.sno = sno;
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

  public String getMajor() {
    return major;
  }

  public void setMajor(String major) {
    this.major = major;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }
}
