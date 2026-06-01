package com.nexeval.dto;

import java.util.List;

public record WeaknessEvaluateRequest(
  WeaknessEvaluateQuestion question,
  String userAnswer
) {

  public record WeaknessEvaluateQuestion(
    String type,
    String stem,
    List<String> options,
    String correctAnswer
  ) {}
}
