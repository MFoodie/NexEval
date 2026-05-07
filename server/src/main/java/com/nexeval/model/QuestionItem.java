package com.nexeval.model;

import java.math.BigDecimal;
import java.util.List;

public record QuestionItem(
	String id,
	String stem,
	String imagePath,
	BigDecimal imageMode,
	List<String> options,
	String answerKey,
	double difficulty,
	QuestionType type,
	int points,
	boolean scorable
) {
}
