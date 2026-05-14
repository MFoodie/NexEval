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
	double difficultyB,
	double discriminationA,
	QuestionType type,
	int points,
	boolean scorable
) {
}
