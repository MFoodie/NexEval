package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "essay_question_bank")
public class EssayQuestionBank {

  @Id
  @Column(name = "id", nullable = false, length = 32)
  private String id;

  @Column(name = "stem", nullable = false, length = 512)
  private String stem;

  @Column(name = "image_path", length = 255)
  private String imagePath;

  @Column(name = "image_mode", precision = 3, scale = 2)
  private BigDecimal imageMode;

  @Column(name = "points", nullable = false)
  private int points;

  @Column(name = "standard_answer", length = 1024)
  private String standardAnswer;

  @Column(name = "scoring_rubric", length = 2048)
  private String scoringRubric;

  @Column(name = "difficulty", nullable = false)
  private double difficulty;

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

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public BigDecimal getImageMode() {
    return imageMode;
  }

  public void setImageMode(BigDecimal imageMode) {
    this.imageMode = imageMode;
  }

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  public String getStandardAnswer() {
    return standardAnswer;
  }

  public void setStandardAnswer(String standardAnswer) {
    this.standardAnswer = standardAnswer;
  }

  public String getScoringRubric() {
    return scoringRubric;
  }

  public void setScoringRubric(String scoringRubric) {
    this.scoringRubric = scoringRubric;
  }

  public double getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(double difficulty) {
    this.difficulty = difficulty;
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
