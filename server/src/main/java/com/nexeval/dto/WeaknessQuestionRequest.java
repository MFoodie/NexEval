package com.nexeval.dto;

import java.util.List;

public record WeaknessQuestionRequest(
  String prompt,
  String courseNo,
  String courseName,
  List<String> weakPoints,
  List<WeaknessQuestionHistory> history
) {

  public record WeaknessQuestionHistory(
    String id,
    String stem,
    Boolean correct,
    String userAnswer,
    String correctAnswer
  ) {}
}
