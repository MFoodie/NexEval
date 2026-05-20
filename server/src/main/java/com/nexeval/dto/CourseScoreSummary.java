package com.nexeval.dto;

public record CourseScoreSummary(
  String cno,
  String cname,
  String eid,
  String teacherName,
  Integer grade,
  Integer classMax,
  Integer classMin,
  Double classAvg
) {}
