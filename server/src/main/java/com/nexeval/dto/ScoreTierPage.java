package com.nexeval.dto;

import java.util.List;

public record ScoreTierPage(
  List<TierStudent> students,
  long totalCount,
  int page,
  int pageSize
) {
  public record TierStudent(
    String userId,
    String sno,
    String name,
    boolean sex,
    int score,
    int maxScore,
    double percent
  ) {}
}
