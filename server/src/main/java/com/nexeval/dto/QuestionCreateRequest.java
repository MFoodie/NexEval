package com.nexeval.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuestionCreateRequest(
  String teacherEid,
  String cno,
  String questionType,
  String stem,
  Integer points,
  String difficulty,
  String imagePath,
  BigDecimal imageMode,
  List<String> options,
  String answerKey,
  String standardAnswer,
  String scoringRubric
) {
}
