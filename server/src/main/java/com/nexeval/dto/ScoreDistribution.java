package com.nexeval.dto;

import java.util.List;

public record ScoreDistribution(
  List<TierInfo> tiers,
  int totalStudents
) {
  public record TierInfo(
    String label,
    int minPercent,
    int maxPercent,
    int count,
    String color
  ) {}
}
