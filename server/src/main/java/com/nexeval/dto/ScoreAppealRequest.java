package com.nexeval.dto;

public record ScoreAppealRequest(
  String userId,
  String courseNo,
  String reason
) {
}
