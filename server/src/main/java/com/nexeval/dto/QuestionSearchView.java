package com.nexeval.dto;

public record QuestionSearchView(
  String id,
  String stem,
  String questionType,
  String difficulty,
  int points,
  String cno
) {
}
