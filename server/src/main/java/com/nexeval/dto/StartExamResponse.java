package com.nexeval.dto;

public record StartExamResponse(
	String sessionId,
	String userId,
	String examId,
	double theta,
	double standardError,
	int maxQuestions,
	String mode,
	Integer timeLimitSeconds,
	String startedAt
) {
}
