package com.nexeval.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionAIDraftResponse(
  String type,
  String stem,
  List<String> options,
  String answerKey,
  String standardAnswer,
  String scoringRubric
) {
}
