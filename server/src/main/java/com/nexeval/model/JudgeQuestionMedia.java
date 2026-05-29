package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "judge_question_media")
public class JudgeQuestionMedia {

  @Id
  @Column(name = "question_id", nullable = false, length = 32)
  private String questionId;

  @Column(name = "image_path", nullable = false, length = 255)
  private String imagePath;

  @Column(name = "image_mode", precision = 3, scale = 2)
  private BigDecimal imageMode;

  public String getQuestionId() {
    return questionId;
  }

  public void setQuestionId(String questionId) {
    this.questionId = questionId;
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
}
