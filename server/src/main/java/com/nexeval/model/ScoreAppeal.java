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
@Table(name = "score_appeal")
public class ScoreAppeal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "session_id", nullable = false, length = 32)
  private String sessionId;

  @Column(name = "user_id", nullable = false, length = 32)
  private String userId;

  @Column(name = "course_no", nullable = false, length = 8)
  private String courseNo;

  @Column(name = "reason", length = 1024)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ScoreAppealStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "handled_at")
  private Instant handledAt;

  @Column(name = "handled_by", length = 32)
  private String handledBy;

  @Column(name = "handled_note", length = 255)
  private String handledNote;

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

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public ScoreAppealStatus getStatus() {
    return status;
  }

  public void setStatus(ScoreAppealStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getHandledAt() {
    return handledAt;
  }

  public void setHandledAt(Instant handledAt) {
    this.handledAt = handledAt;
  }

  public String getHandledBy() {
    return handledBy;
  }

  public void setHandledBy(String handledBy) {
    this.handledBy = handledBy;
  }

  public String getHandledNote() {
    return handledNote;
  }

  public void setHandledNote(String handledNote) {
    this.handledNote = handledNote;
  }
}
