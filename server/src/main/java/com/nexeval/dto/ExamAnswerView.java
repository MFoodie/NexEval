package com.nexeval.dto;

public record ExamAnswerView(
  String questionId,
  String answerText,
  String type,
  Boolean correct
) {
}
