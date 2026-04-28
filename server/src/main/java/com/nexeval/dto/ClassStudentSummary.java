package com.nexeval.dto;

public record ClassStudentSummary(
  String userId,
  String sno,
  String name,
  boolean sex,
  Integer grade
) {
}
