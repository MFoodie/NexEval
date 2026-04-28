package com.nexeval.dto;

import java.util.List;

public record TeacherClassSummary(
  String cno,
  String cname,
  String eid,
  List<ClassStudentSummary> students
) {
}
