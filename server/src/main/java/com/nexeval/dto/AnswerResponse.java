package com.nexeval.dto;

public record AnswerResponse(boolean correct, double theta, int answeredCount, boolean finished) {
}
