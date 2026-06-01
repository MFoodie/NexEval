package com.nexeval.dto;

import java.util.List;

public record WeaknessTrainingResponse(
  List<WeaknessQuestionResponse> questions
) {
}
