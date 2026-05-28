package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "paper_question_item")
public class PaperQuestionItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "paper_id", nullable = false, length = 32)
  private String paperId;

  @Column(name = "question_id", nullable = false, length = 32)
  private String questionId;

  @Column(name = "question_type", nullable = false, length = 16)
  private String questionType;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPaperId() { return paperId; }
  public void setPaperId(String paperId) { this.paperId = paperId; }
  public String getQuestionId() { return questionId; }
  public void setQuestionId(String questionId) { this.questionId = questionId; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public int getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
