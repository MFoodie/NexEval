package com.nexeval.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "exam_answer")
public class ExamAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "session_id", nullable = false, length = 32)
  private String sessionId;

  @Column(name = "user_id", nullable = false, length = 32)
  private String userId;

  @Column(name = "course_no", length = 8)
  private String courseNo;

  @Column(name = "question_id", nullable = false, length = 32)
  private String questionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_type", nullable = false, length = 16)
  private QuestionType questionType;

  @Column(name = "answer_text", length = 1024)
  private String answerText;

  @Column(name = "answer_image_path", length = 255)
  private String answerImagePath;

  @Column(name = "correct")
  private Boolean correct;

  @Column(name = "score")
  private Integer score;

  @Column(name = "review_note", length = 255)
  private String reviewNote;

  @Column(name = "reviewed", nullable = false)
  private boolean reviewed;

  @Column(name = "reviewer_id", length = 32)
  private String reviewerId;

  @Column(name = "answered_at", nullable = false)
  private Instant answeredAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCourseNo() {
    return courseNo;
  }

  public void setCourseNo(String courseNo) {
    this.courseNo = courseNo;
  }

  public String getQuestionId() {
    return questionId;
  }

  public void setQuestionId(String questionId) {
    this.questionId = questionId;
  }

  public QuestionType getQuestionType() {
    return questionType;
  }

  public void setQuestionType(QuestionType questionType) {
    this.questionType = questionType;
  }

  public String getAnswerText() {
    return answerText;
  }

  public void setAnswerText(String answerText) {
    this.answerText = answerText;
  }

  public String getAnswerImagePath() {
    return answerImagePath;
  }

  public void setAnswerImagePath(String answerImagePath) {
    this.answerImagePath = answerImagePath;
  }

  public Boolean getCorrect() {
    return correct;
  }

  public void setCorrect(Boolean correct) {
    this.correct = correct;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public void setReviewNote(String reviewNote) {
    this.reviewNote = reviewNote;
  }

  public boolean isReviewed() {
    return reviewed;
  }

  public void setReviewed(boolean reviewed) {
    this.reviewed = reviewed;
  }

  public String getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(String reviewerId) {
    this.reviewerId = reviewerId;
  }

  public Instant getAnsweredAt() {
    return answeredAt;
  }

  public void setAnsweredAt(Instant answeredAt) {
    this.answeredAt = answeredAt;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }
}
