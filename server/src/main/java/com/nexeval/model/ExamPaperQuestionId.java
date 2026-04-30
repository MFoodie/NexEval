package com.nexeval.model;

import java.io.Serializable;
import java.util.Objects;

public class ExamPaperQuestionId implements Serializable {

  private String paper;
  private String question;

  public ExamPaperQuestionId() {
  }

  public ExamPaperQuestionId(String paper, String question) {
    this.paper = paper;
    this.question = question;
  }

  public String getPaper() {
    return paper;
  }

  public void setPaper(String paper) {
    this.paper = paper;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    ExamPaperQuestionId that = (ExamPaperQuestionId) other;
    return Objects.equals(paper, that.paper) && Objects.equals(question, that.question);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paper, question);
  }
}
