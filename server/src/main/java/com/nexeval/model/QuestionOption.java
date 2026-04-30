package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_option")
public class QuestionOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", nullable = false)
  private QuestionBank question;

  @Column(name = "option_text", nullable = false, length = 255)
  private String optionText;

  @Column(name = "option_order", nullable = false)
  private int optionOrder;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public QuestionBank getQuestion() {
    return question;
  }

  public void setQuestion(QuestionBank question) {
    this.question = question;
  }

  public String getOptionText() {
    return optionText;
  }

  public void setOptionText(String optionText) {
    this.optionText = optionText;
  }

  public int getOptionOrder() {
    return optionOrder;
  }

  public void setOptionOrder(int optionOrder) {
    this.optionOrder = optionOrder;
  }
}
