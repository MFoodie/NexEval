package com.nexeval.dto;

import java.util.List;

public record WeaknessExplainRequest(
  String prompt,
  WeaknessExplainQuestion question
) {

  public record WeaknessExplainQuestion(
    String stem,
    List<String> options,
    String correctAnswer,
    String userAnswer
  ) {}
}
