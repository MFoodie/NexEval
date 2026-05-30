package com.nexeval.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatKnowledgeInsightsResponse(
  List<MasteryPoint> masteryPoints,
  List<String> weakPoints
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MasteryPoint(
    String point,
    Integer score
  ) {}
}
