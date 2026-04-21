package com.nexeval.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExamSession {

  private final String sessionId;
  private final String userId;
  private final int maxQuestions;
  private final Set<String> answeredQuestionIds = new HashSet<>();

  private int answeredCount;
  private int correctCount;
  private double theta;
  private boolean finished;

  public ExamSession(String sessionId, String userId, int maxQuestions) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.maxQuestions = maxQuestions;
    this.theta = 0.0;
    this.finished = false;
  }

  public synchronized void markAnswered(String questionId, boolean correct, double questionDifficulty) {
    if (finished) {
      throw new IllegalStateException("Exam session already finished.");
    }

    if (answeredQuestionIds.contains(questionId)) {
      throw new IllegalArgumentException("Question already answered.");
    }

    answeredQuestionIds.add(questionId);
    answeredCount++;

    if (correct) {
      correctCount++;
    }

    updateTheta(correct, questionDifficulty);

    if (answeredCount >= maxQuestions) {
      finished = true;
    }
  }

  public synchronized void finish() {
    finished = true;
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

  public synchronized Set<String> getAnsweredQuestionIds() {
    return Collections.unmodifiableSet(new HashSet<>(answeredQuestionIds));
  }
}
