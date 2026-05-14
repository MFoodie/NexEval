package com.nexeval.dto;

public record AnswerResponse(boolean correct, double theta, double standardError, int answeredCount, boolean finished) {
}
