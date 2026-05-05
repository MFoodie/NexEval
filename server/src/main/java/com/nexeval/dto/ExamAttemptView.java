package com.nexeval.dto;

public record ExamAttemptView(
  String sessionId,
  String userId,
  String courseNo,
  String mode,
  String status,
  String startedAt,
  String submittedAt,
  Integer timeLimitSeconds,
  Integer totalScore
) {
}
