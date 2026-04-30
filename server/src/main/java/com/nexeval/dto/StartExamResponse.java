package com.nexeval.dto;

public record StartExamResponse(String sessionId, String userId, String examId, double theta, int maxQuestions) {
}
