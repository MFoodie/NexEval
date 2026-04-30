package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_paper_question")
@IdClass(ExamPaperQuestionId.class)
public class ExamPaperQuestion {

  @Id
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "paper_id", nullable = false)
  private ExamPaper paper;

  @Id
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "question_id", nullable = false)
  private QuestionBank question;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public ExamPaper getPaper() {
    return paper;
  }

  public void setPaper(ExamPaper paper) {
    this.paper = paper;
  }

  public QuestionBank getQuestion() {
    return question;
  }

  public void setQuestion(QuestionBank question) {
    this.question = question;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
