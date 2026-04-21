package com.nexeval.dto;

public record NextQuestionResponse(
  String sessionId,
  int answeredCount,
  int maxQuestions,
  double theta,
  boolean finished,
  QuestionView question
) {
}
