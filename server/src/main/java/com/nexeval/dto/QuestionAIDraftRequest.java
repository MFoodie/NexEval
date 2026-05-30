package com.nexeval.dto;

public record QuestionAIDraftRequest(
  String teacherEid,
  String cno,
  String courseName,
  String questionType,
  Integer points,
  String difficulty
) {
}
