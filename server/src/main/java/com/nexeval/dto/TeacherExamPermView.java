package com.nexeval.dto;

public record TeacherExamPermView(
  String eid,
  String userId,
  String name,
  String title,
  String department,
  boolean canCreateExam
) {
}
