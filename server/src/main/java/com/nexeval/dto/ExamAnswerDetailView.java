package com.nexeval.dto;

import java.util.List;

public record ExamAnswerDetailView(
  Long answerId,
  String questionId,
  String stem,
  String type,
  String answerText,
  String answerImagePath,
  String questionImagePath,
  List<String> options,
  String correctAnswer,
  Boolean correct,
  Integer score,
  Boolean reviewed,
  String reviewNote,
  String aiReviewLog,
  Integer maxScore
) {
}
