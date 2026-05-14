package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "judge_question_bank")
public class JudgeQuestionBank {

  @Id
  @Column(name = "id", nullable = false, length = 32)
  private String id;

  @Column(name = "stem", nullable = false, length = 512)
  private String stem;

  @Column(name = "answer_key", nullable = false)
  private boolean answerKey;

  @Column(name = "points", nullable = false)
  private int points;

  @Column(name = "difficulty", nullable = false)
  private double difficulty;

  @Column(name = "difficulty_b", nullable = false)
  private double difficultyB;

  @Column(name = "discrimination_a", nullable = false)
  private double discriminationA;

  @Column(name = "cno", length = 8)
  private String cno;

  @Column(name = "active", nullable = false)
  private boolean active = true;

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

  public boolean isAnswerKey() {
    return answerKey;
  }

  public void setAnswerKey(boolean answerKey) {
    this.answerKey = answerKey;
  }

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  public double getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(double difficulty) {
    this.difficulty = difficulty;
  }

  public double getDifficultyB() {
    return difficultyB;
  }

  public void setDifficultyB(double difficultyB) {
    this.difficultyB = difficultyB;
  }

  public double getDiscriminationA() {
    return discriminationA;
  }

  public void setDiscriminationA(double discriminationA) {
    this.discriminationA = discriminationA;
  }

  public String getCno() {
    return cno;
  }

  public void setCno(String cno) {
    this.cno = cno;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
