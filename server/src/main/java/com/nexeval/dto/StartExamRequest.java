package com.nexeval.dto;

import jakarta.validation.constraints.NotBlank;

public record StartExamRequest(@NotBlank(message = "userId is required") String userId) {
}
