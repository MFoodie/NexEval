package com.nexeval.dto;

public record ExamAnswerDetailView(
  Long answerId,
  String questionId,
  String stem,
  String type,
  String answerText,
  Boolean correct,
  Integer score,
  Boolean reviewed,
  String reviewNote,
  Integer maxScore
) {
}
