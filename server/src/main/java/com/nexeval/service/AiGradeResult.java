package com.nexeval.service;

public record AiGradeResult(int score, String comment, double confidence, String aiLog) {
}
