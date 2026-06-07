package com.nexeval.dto;

public record CourseMemberSummary(
  String userId,
  String name,
  String avatarUrl,
  boolean teacher
) {
}
