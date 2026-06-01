package com.nexeval.dto;

import java.util.List;

public record WeaknessTrainingRequest(
  String courseNo,
  String courseName,
  List<String> weakPoints,
  List<WeaknessQuestionRequest.WeaknessQuestionHistory> history,
  Integer choiceCount,
  Integer judgeCount,
  Integer blankCount,
  Integer essayCount
) {
}
