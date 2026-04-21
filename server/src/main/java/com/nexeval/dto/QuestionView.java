package com.nexeval.dto;

import java.util.List;

public record QuestionView(String id, String stem, List<String> options, double difficulty) {
}
