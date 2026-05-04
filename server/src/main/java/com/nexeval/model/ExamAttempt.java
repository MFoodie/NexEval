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
@Table(name = "exam_attempt")
public class ExamAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "session_id", nullable = false, length = 32, unique = true)
  private String sessionId;

  @Column(name = "user_id", nullable = false, length = 32)
  private String userId;

  @Column(name = "course_no", length = 8)
  private String courseNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 16)
  private SessionMode mode;

  @Column(name = "paper_id", length = 32)
  private String paperId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ExamAttemptStatus status;

  @Column(name = "time_limit_seconds")
  private Integer timeLimitSeconds;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

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

  public SessionMode getMode() {
    return mode;
  }

  public void setMode(SessionMode mode) {
    this.mode = mode;
  }

  public String getPaperId() {
    return paperId;
  }

  public void setPaperId(String paperId) {
    this.paperId = paperId;
  }

  public ExamAttemptStatus getStatus() {
    return status;
  }

  public void setStatus(ExamAttemptStatus status) {
    this.status = status;
  }

  public Integer getTimeLimitSeconds() {
    return timeLimitSeconds;
  }

  public void setTimeLimitSeconds(Integer timeLimitSeconds) {
    this.timeLimitSeconds = timeLimitSeconds;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }
}
