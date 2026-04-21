package com.nexeval.model;

import java.util.List;

public record QuestionItem(String id, String stem, List<String> options, String answerKey, double difficulty) {
}
