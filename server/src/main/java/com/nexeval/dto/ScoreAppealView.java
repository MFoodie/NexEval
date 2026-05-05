package com.nexeval.dto;

public record ScoreAppealView(
  Long id,
  String sessionId,
  String userId,
  String courseNo,
  String reason,
  String status,
  String createdAt,
  String handledAt,
  String handledBy,
  String handledNote
) {
}
