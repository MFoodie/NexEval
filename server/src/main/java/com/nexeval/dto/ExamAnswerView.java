package com.nexeval.dto;

public record ExamAnswerView(
  String questionId,
  String answerText,
  String answerImagePath,
  String type,
  Boolean correct
) {
}
