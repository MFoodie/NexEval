package com.nexeval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexeval.dto.CatKnowledgeInsightsResponse;
import com.nexeval.dto.ExamAnswerDetailView;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiWeaknessServiceTest {

  @Test
  void calculatesKnowledgePointScoreFromActualAnswers() {
    AiWeaknessService service = new AiWeaknessService(new ObjectMapper(), "", "qwen-plus", 1000);
    List<ExamAnswerDetailView> answers = List.of(
      answer("【知识点：流水线】题目一", true),
      answer("【知识点：流水线】题目二", false),
      answer("【知识点：Cache】题目三", false)
    );

    CatKnowledgeInsightsResponse result =
      service.generateCatKnowledgeInsights("COURSE01", "Course", answers);

    CatKnowledgeInsightsResponse.MasteryPoint pipeline = result.masteryPoints().stream()
      .filter(item -> "流水线".equals(item.point()))
      .findFirst()
      .orElseThrow();

    assertEquals(50, pipeline.score());
    assertEquals(1, pipeline.correctCount());
    assertEquals(2, pipeline.questionCount());
    assertFalse(pipeline.inferred());
  }

  @Test
  void allWrongAnswersCannotProducePositiveInferredMastery() {
    AiWeaknessService service = new AiWeaknessService(new ObjectMapper(), "", "qwen-plus", 1000);
    List<ExamAnswerDetailView> answers = List.of(
      answer("Question without a knowledge label", false)
    );
    CatKnowledgeInsightsResponse aiPayload = new CatKnowledgeInsightsResponse(
      List.of(new CatKnowledgeInsightsResponse.MasteryPoint("Pipeline", 76, null, null, true)),
      List.of("Pipeline")
    );

    CatKnowledgeInsightsResponse result = service.sanitizeCatKnowledgeInsights(aiPayload, answers);

    assertTrue(result.masteryPoints().stream().allMatch(item -> item.score() == 0));
  }

  private ExamAnswerDetailView answer(String stem, boolean correct) {
    return new ExamAnswerDetailView(
      1L,
      "question",
      stem,
      "choice",
      "A",
      null,
      null,
      List.of("A", "B"),
      "A",
      correct,
      correct ? 1 : 0,
      true,
      null,
      null,
      1,
      2.5,
      null
    );
  }
}
