package com.nexeval.dto;

public record WeaknessEvaluateResponse(
  Boolean correct,
  Integer score,
  String correctAnswer,
  String explanation
) {
}
