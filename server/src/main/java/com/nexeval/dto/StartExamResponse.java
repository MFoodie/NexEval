package com.nexeval.dto;

public record StartExamResponse(String sessionId, String userId, double theta, int maxQuestions) {
}
