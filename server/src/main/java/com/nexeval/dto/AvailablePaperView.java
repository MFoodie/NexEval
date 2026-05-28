package com.nexeval.dto;

public record AvailablePaperView(
  String definitionId,
  String paperId,
  String paperName,
  String description,
  int durationMinutes,
  int questionCount,
  String cno,
  String eid
) {
}
