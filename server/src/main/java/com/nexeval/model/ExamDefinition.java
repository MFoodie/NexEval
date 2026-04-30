package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_definition")
public class ExamDefinition {

  @Id
  @Column(name = "id", nullable = false, length = 32)
  private String id;

  @Column(name = "name", nullable = false, length = 64)
  private String name;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "max_questions", nullable = false)
  private int maxQuestions;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault = false;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "paper_id", nullable = false)
  private ExamPaper paper;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getMaxQuestions() {
    return maxQuestions;
  }

  public void setMaxQuestions(int maxQuestions) {
    this.maxQuestions = maxQuestions;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  public ExamPaper getPaper() {
    return paper;
  }

  public void setPaper(ExamPaper paper) {
    this.paper = paper;
  }
}
