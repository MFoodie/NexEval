package com.nexeval.dto;

public record TeacherVipView(
  String eid,
  String userId,
  String name,
  String title,
  String department,
  boolean vip
) {
}
