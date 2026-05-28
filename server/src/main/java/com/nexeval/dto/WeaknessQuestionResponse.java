package com.nexeval.dto;

import java.util.List;

public record WeaknessQuestionResponse(
  String id,
  String type,
  String stem,
  List<String> options,
  String correctAnswer,
  List<String> knowledgePoints,
  String imagePath,
  String explanation
) {}
