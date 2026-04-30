package com.nexeval.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_bank")
public class QuestionBank {

  @Id
  @Column(name = "id", nullable = false, length = 32)
  private String id;

  @Column(name = "stem", nullable = false, length = 512)
  private String stem;

  @Column(name = "answer_key", nullable = false, length = 64)
  private String answerKey;

  @Column(name = "difficulty", nullable = false)
  private double difficulty;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @OrderBy("optionOrder ASC")
  private List<QuestionOption> options = new ArrayList<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStem() {
    return stem;
  }

  public void setStem(String stem) {
    this.stem = stem;
  }

  public String getAnswerKey() {
    return answerKey;
  }

  public void setAnswerKey(String answerKey) {
    this.answerKey = answerKey;
  }

  public double getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(double difficulty) {
    this.difficulty = difficulty;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<QuestionOption> getOptions() {
    return options;
  }

  public void setOptions(List<QuestionOption> options) {
    this.options = options == null ? new ArrayList<>() : options;
  }
}
