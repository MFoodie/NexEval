package com.nexeval.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
  @NotBlank(message = "questionId is required") String questionId,
  @NotBlank(message = "selectedOption is required") String selectedOption
) {
}
