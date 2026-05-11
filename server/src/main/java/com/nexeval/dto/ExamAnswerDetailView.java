package com.nexeval.dto;

public record ExamAnswerDetailView(
  Long answerId,
  String questionId,
  String stem,
  String type,
  String answerText,
  String answerImagePath,
  Boolean correct,
  Integer score,
  Boolean reviewed,
  String reviewNote,
  String aiReviewLog,
  Integer maxScore
) {
}
