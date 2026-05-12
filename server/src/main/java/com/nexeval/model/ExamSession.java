package com.nexeval.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExamSession {

  private final String sessionId;
  private final String userId;
  private final String courseNo;
  private final String examId;
  private final int maxQuestions;
  private final SessionMode mode;
  private final String paperId;
  private final Instant startedAt;
  private final Integer timeLimitSeconds;
  private Instant submittedAt;
  private final Set<String> answeredQuestionIds = new HashSet<>();

  private int answeredCount;
  private int correctCount;
  private double theta;
  private boolean finished;

  public ExamSession(
    String sessionId,
    String userId,
    String courseNo,
    String examId,
    int maxQuestions,
    SessionMode mode,
    String paperId,
    Instant startedAt,
    Integer timeLimitSeconds
  ) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.courseNo = courseNo;
    this.examId = examId;
    this.maxQuestions = maxQuestions;
    this.mode = mode;
    this.paperId = paperId;
    this.startedAt = startedAt == null ? Instant.now() : startedAt;
    this.timeLimitSeconds = timeLimitSeconds;
    this.theta = 0.0;
    this.finished = false;
  }

  public synchronized void markAnswered(
    String questionId,
    boolean correct,
    double questionDifficulty,
    boolean scoreEnabled
  ) {
    if (finished) {
      throw new IllegalStateException("Exam session already finished.");
    }

    if (answeredQuestionIds.contains(questionId)) {
      // Allow re-submit without changing session statistics.
      return;
    }

    answeredQuestionIds.add(questionId);
    answeredCount++;

    if (correct) {
      correctCount++;
    }

    if (scoreEnabled) {
      updateTheta(correct, questionDifficulty);
    }

  }

  public synchronized void finish() {
    submit(Instant.now());
  }

  private void updateTheta(boolean correct, double questionDifficulty) {
    double step = correct ? 0.35 : -0.35;

    if (Math.abs(theta - questionDifficulty) > 1.0) {
      step *= 0.8;
    }

    theta = clamp(theta + step, -3.0, 3.0);
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(value, max));
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getUserId() {
    return userId;
  }

  public String getCourseNo() {
    return courseNo;
  }

  public String getExamId() {
    return examId;
  }

  public SessionMode getMode() {
    return mode;
  }

  public String getPaperId() {
    return paperId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Integer getTimeLimitSeconds() {
    return timeLimitSeconds;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public synchronized int getAnsweredCount() {
    return answeredCount;
  }

  public synchronized int getCorrectCount() {
    return correctCount;
  }

  public synchronized double getTheta() {
    return theta;
  }

  public int getMaxQuestions() {
    return maxQuestions;
  }

  public synchronized boolean isFinished() {
    return finished;
  }

  public synchronized void submit(Instant now) {
    finished = true;
    submittedAt = now == null ? Instant.now() : now;
  }

  public synchronized boolean isExpired(Instant now) {
    if (timeLimitSeconds == null) {
      return false;
    }
    Instant base = startedAt == null ? Instant.now() : startedAt;
    Instant current = now == null ? Instant.now() : now;
    return Duration.between(base, current).getSeconds() >= timeLimitSeconds;
  }

  public synchronized long getRemainingSeconds(Instant now) {
    if (timeLimitSeconds == null) {
      return -1;
    }
    Instant base = startedAt == null ? Instant.now() : startedAt;
    Instant current = now == null ? Instant.now() : now;
    long elapsed = Duration.between(base, current).getSeconds();
    return Math.max(0, timeLimitSeconds - elapsed);
  }

  public synchronized Set<String> getAnsweredQuestionIds() {
    return Collections.unmodifiableSet(new HashSet<>(answeredQuestionIds));
  }
}
