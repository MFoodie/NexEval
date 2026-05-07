package com.nexeval.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuestionView(String id, String stem, String imagePath, BigDecimal imageMode, List<String> options, double difficulty, String type) {
}
