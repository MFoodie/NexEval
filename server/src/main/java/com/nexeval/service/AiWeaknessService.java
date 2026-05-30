package com.nexeval.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexeval.dto.CatKnowledgeInsightsResponse;
import com.nexeval.dto.ExamAnswerDetailView;
import com.nexeval.dto.QuestionAIDraftResponse;
import com.nexeval.dto.WeaknessExplainRequest;
import com.nexeval.dto.WeaknessExplainResponse;
import com.nexeval.dto.WeaknessQuestionRequest;
import com.nexeval.dto.WeaknessQuestionResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiWeaknessService {

  private static final Logger log = LoggerFactory.getLogger(AiWeaknessService.class);

  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public AiWeaknessService(
    ObjectMapper objectMapper,
    @Value("${dashscope.api-key:}") String apiKey,
    @Value("${dashscope.model:qwen-plus}") String model
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null ? "qwen-plus" : model.trim();
  }

  public WeaknessQuestionResponse generateQuestion(WeaknessQuestionRequest request) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key 未配置");
    }
    if (request == null) {
      throw new IllegalArgumentException("请求体不能为空");
    }

    String prompt = buildQuestionPrompt(request);
    String rawText = callDashscope(prompt, "你是出题助手，必须严格按要求返回 JSON。", "AI 出题生成失败");
    String jsonText = extractJson(rawText);

    try {
      WeaknessQuestionResponse payload = objectMapper.readValue(jsonText, WeaknessQuestionResponse.class);
      if (payload == null || payload.stem() == null || payload.stem().isBlank()) {
        throw new IllegalStateException("AI 返回题干为空");
      }
      return payload;
    } catch (IOException ex) {
      throw new IllegalStateException("AI 返回不是有效 JSON");
    }
  }

  public WeaknessExplainResponse generateExplanation(WeaknessExplainRequest request) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key 未配置");
    }
    if (request == null || request.question() == null) {
      throw new IllegalArgumentException("题目信息不能为空");
    }

    String prompt = buildExplainPrompt(request);
    String text = callDashscope(prompt, "你是错题解析助手，请输出简洁中文解析。", "AI 解析生成失败");
    return new WeaknessExplainResponse(text.trim());
  }

  public CatKnowledgeInsightsResponse generateCatKnowledgeInsights(
    String courseNo,
    String courseName,
    List<ExamAnswerDetailView> answers
  ) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key 鏈厤缃?");
    }

    List<ExamAnswerDetailView> safeAnswers = answers == null ? List.of() : answers;
    if (safeAnswers.isEmpty()) {
      return new CatKnowledgeInsightsResponse(List.of(), List.of());
    }

    String prompt = buildCatKnowledgePrompt(courseNo, courseName, safeAnswers);
    String rawText = callDashscope(
      prompt,
      "You are a CAT learning-diagnosis assistant. Infer concrete knowledge points and return strict JSON only.",
      "AI CAT knowledge analysis failed"
    );
    String jsonText = extractJson(rawText);

    try {
      CatKnowledgeInsightsResponse payload = objectMapper.readValue(jsonText, CatKnowledgeInsightsResponse.class);
      return sanitizeCatKnowledgeInsights(payload, safeAnswers);
    } catch (IOException ex) {
      throw new IllegalStateException("AI 杩斿洖涓嶆槸鏈夋晥 JSON");
    }
  }

  public QuestionAIDraftResponse generateTeacherQuestionDraft(
    String courseNo,
    String courseName,
    String questionType,
    Integer points,
    String difficulty
  ) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key unavailable");
    }

    String prompt = buildTeacherQuestionDraftPrompt(courseNo, courseName, questionType, points, difficulty);
    String rawText = callDashscope(
      prompt,
      "You are a question-generation assistant for teachers. Return strict JSON only.",
      "AI question draft generation failed"
    );
    String jsonText = extractJson(rawText);

    try {
      QuestionAIDraftResponse payload = objectMapper.readValue(jsonText, QuestionAIDraftResponse.class);
      return sanitizeTeacherQuestionDraft(payload, questionType);
    } catch (IOException ex) {
      throw new IllegalStateException("AI question draft is not valid JSON");
    }
  }

  private String callDashscope(String prompt, String systemRole, String errorMessage) {
    MultiModalMessage systemMessage = MultiModalMessage.builder()
      .role(Role.SYSTEM.getValue())
      .content(List.of(Map.of("text", systemRole)))
      .build();
    MultiModalMessage userMessage = MultiModalMessage.builder()
      .role(Role.USER.getValue())
      .content(List.of(Map.of("text", prompt)))
      .build();

    MultiModalConversationParam param = MultiModalConversationParam.builder()
      .apiKey(apiKey)
      .model(model)
      .messages(List.of(systemMessage, userMessage))
      .modalities(Collections.singletonList("text"))
      .incrementalOutput(false)
      .build();

    try {
      MultiModalConversation conversation = new MultiModalConversation();
      MultiModalConversationResult result = conversation.call(param);
      return extractText(result);
    } catch (ApiException | NoApiKeyException | UploadFileException ex) {
      log.warn("AI weakness generation failed: {}", ex.getMessage());
      throw new IllegalStateException(errorMessage);
    }
  }

  private String buildQuestionPrompt(WeaknessQuestionRequest request) {
    StringBuilder builder = new StringBuilder();
    String basePrompt = request.prompt() == null ? "" : request.prompt().trim();

    if (!basePrompt.isBlank()) {
      builder.append(basePrompt).append('\n');
    }

    builder.append("课程编号：").append(normalize(request.courseNo())).append('\n');
    builder.append("课程名称：").append(normalize(request.courseName())).append('\n');

    List<String> weakPoints = request.weakPoints() == null ? List.of() : request.weakPoints();
    if (!weakPoints.isEmpty()) {
      builder.append("薄弱知识点：").append(String.join("，", weakPoints)).append('\n');
    }

    List<WeaknessQuestionRequest.WeaknessQuestionHistory> history =
      request.history() == null ? List.of() : request.history();
    if (!history.isEmpty()) {
      builder.append("近期作答（用于避免重复）：\n");
      int limit = Math.min(history.size(), 6);
      for (int i = 0; i < limit; i++) {
        WeaknessQuestionRequest.WeaknessQuestionHistory item = history.get(i);
        builder.append(i + 1)
          .append(". ")
          .append(normalize(item.stem()))
          .append(" | 你的答案=")
          .append(normalize(item.userAnswer()))
          .append(" | 正确答案=")
          .append(normalize(item.correctAnswer()))
          .append('\n');
      }
    }

    builder.append("请返回 JSON，字段包含: id, type, stem, options, correctAnswer, knowledgePoints, imagePath, explanation。\n");
    builder.append("要求：\n");
    builder.append("1. type 仅限 choice 或 judge。\n");
    builder.append("2. options 为字符串数组，judge 题 options 必须是 [\"true\", \"false\"]。\n");
    builder.append("3. correctAnswer 使用 options 原文或选项字母。\n");
    builder.append("4. 只输出 JSON，不要解释。\n");
    return builder.toString();
  }

  private String buildExplainPrompt(WeaknessExplainRequest request) {
    StringBuilder builder = new StringBuilder();
    String basePrompt = request.prompt() == null ? "" : request.prompt().trim();
    if (!basePrompt.isBlank()) {
      builder.append(basePrompt).append('\n');
    }

    WeaknessExplainRequest.WeaknessExplainQuestion question = request.question();
    builder.append("题干：").append(normalize(question.stem())).append('\n');

    List<String> options = question.options() == null ? List.of() : question.options();
    if (!options.isEmpty()) {
      builder.append("选项：").append(String.join(" | ", options)).append('\n');
    }

    builder.append("正确答案：").append(normalize(question.correctAnswer())).append('\n');
    builder.append("学生答案：").append(normalize(question.userAnswer())).append('\n');
    builder.append("请输出简洁中文解析，指出错因并给出正确思路。\n");
    return builder.toString();
  }

  private String buildCatKnowledgePrompt(
    String courseNo,
    String courseName,
    List<ExamAnswerDetailView> answers
  ) {
    int total = answers.size();
    long correctCount = answers.stream().filter(item -> Boolean.TRUE.equals(item.correct())).count();
    long wrongCount = total - correctCount;
    int correctRate = total == 0 ? 0 : (int) Math.round(correctCount * 100.0 / total);
    StringBuilder builder = new StringBuilder();
    builder.append("Analyze the following CAT answer history and infer concrete course knowledge points.\n");
    builder.append("Course No: ").append(normalize(courseNo)).append('\n');
    builder.append("Course Name: ").append(normalize(courseName)).append('\n');
    builder.append("Answered: ").append(total).append('\n');
    builder.append("Correct: ").append(correctCount).append('\n');
    builder.append("Wrong: ").append(wrongCount).append('\n');
    builder.append("Correct Rate: ").append(correctRate).append("%\n");
    builder.append("Return JSON only in this format:\n");
    builder.append("{\"masteryPoints\":[{\"point\":\"Point A\",\"score\":82}],\"weakPoints\":[\"Point B\"]}\n");
    builder.append("Requirements:\n");
    builder.append("1. point and weakPoints must be specific knowledge points, not question types like choice/judge/blank/essay.\n");
    builder.append("2. masteryPoints should contain 2 to 4 items with integer score from 0 to 100.\n");
    builder.append("3. weakPoints should contain 1 to 3 weak knowledge points for the current course.\n");
    builder.append("4. Scores must reflect this specific attempt, especially current wrong answers and current correct rate.\n");
    builder.append("5. Avoid fixed template scores such as 90,88,85,82 unless the current attempt truly supports them.\n");
    builder.append("6. Return JSON only, with no markdown.\n");
    builder.append("Wrong answer evidence:\n");

    int wrongIndex = 0;
    for (ExamAnswerDetailView item : answers) {
      if (Boolean.TRUE.equals(item.correct())) {
        continue;
      }
      wrongIndex += 1;
      builder.append("W").append(wrongIndex)
        .append(". stem=").append(compactText(item.stem(), 120))
        .append("; userAnswer=").append(compactText(item.answerText(), 40))
        .append("; correctAnswer=").append(compactText(item.correctAnswer(), 40))
        .append('\n');
    }

    builder.append("Correct answer evidence:\n");

    int limit = Math.min(answers.size(), 20);
    for (int i = 0; i < limit; i++) {
      ExamAnswerDetailView item = answers.get(i);
      if (!Boolean.TRUE.equals(item.correct())) {
        continue;
      }
      builder.append(i + 1)
        .append(". stem=").append(compactText(item.stem(), 120))
        .append("; correctAnswer=").append(compactText(item.correctAnswer(), 40))
        .append('\n');
    }

    return builder.toString();
  }

  private String buildTeacherQuestionDraftPrompt(
    String courseNo,
    String courseName,
    String questionType,
    Integer points,
    String difficulty
  ) {
    String normalizedType = normalize(questionType).toUpperCase();
    StringBuilder builder = new StringBuilder();
    builder.append("Generate one new exam question draft for a teacher.\n");
    builder.append("Course No: ").append(normalize(courseNo)).append('\n');
    builder.append("Course Name: ").append(normalize(courseName)).append('\n');
    builder.append("Question Type: ").append(normalizedType).append('\n');
    builder.append("Points: ").append(points == null ? 5 : points).append('\n');
    builder.append("Difficulty: ").append(normalize(difficulty)).append('\n');
    builder.append("Return JSON only in this format:\n");
    builder.append("{\"type\":\"CHOICE\",\"stem\":\"...\",\"options\":[\"...\"],\"answerKey\":\"...\",\"standardAnswer\":\"\",\"scoringRubric\":\"\"}\n");
    builder.append("Requirements:\n");
    builder.append("1. The question must match the given course, difficulty, points, and question type.\n");
    builder.append("2. CHOICE: provide exactly 4 options and one correct answer matching one option.\n");
    builder.append("3. JUDGE: provide a stem and answerKey as true or false.\n");
    builder.append("4. BLANK: provide a stem and a concise answerKey.\n");
    builder.append("5. ESSAY: provide a stem only; answerKey can be empty.\n");
    builder.append("6. No markdown, no explanation, JSON only.\n");
    return builder.toString();
  }

  private CatKnowledgeInsightsResponse sanitizeCatKnowledgeInsights(
    CatKnowledgeInsightsResponse payload,
    List<ExamAnswerDetailView> answers
  ) {
    if (payload == null) {
      return new CatKnowledgeInsightsResponse(List.of(), List.of());
    }

    List<CatKnowledgeInsightsResponse.MasteryPoint> masteryPoints = new java.util.ArrayList<>();
    java.util.Set<String> masterySeen = new java.util.LinkedHashSet<>();
    List<CatKnowledgeInsightsResponse.MasteryPoint> rawMastery =
      payload.masteryPoints() == null ? List.of() : payload.masteryPoints();

    for (CatKnowledgeInsightsResponse.MasteryPoint item : rawMastery) {
      String point = normalize(item == null ? null : item.point());
      if (point.isBlank() || !masterySeen.add(point)) {
        continue;
      }
      masteryPoints.add(new CatKnowledgeInsightsResponse.MasteryPoint(
        point,
        clampScore(item == null ? null : item.score())
      ));
      if (masteryPoints.size() >= 4) {
        break;
      }
    }

    masteryPoints.sort((left, right) -> Integer.compare(
      clampScore(right == null ? null : right.score()),
      clampScore(left == null ? null : left.score())
    ));
    masteryPoints = calibrateMasteryScores(masteryPoints, answers);

    List<String> weakPoints = new java.util.ArrayList<>();
    java.util.Set<String> weakSeen = new java.util.LinkedHashSet<>();
    List<String> rawWeakPoints = payload.weakPoints() == null ? List.of() : payload.weakPoints();
    for (String item : rawWeakPoints) {
      String point = normalize(item);
      if (point.isBlank() || !weakSeen.add(point)) {
        continue;
      }
      weakPoints.add(point);
      if (weakPoints.size() >= 3) {
        break;
      }
    }

    return new CatKnowledgeInsightsResponse(masteryPoints, weakPoints);
  }

  private QuestionAIDraftResponse sanitizeTeacherQuestionDraft(QuestionAIDraftResponse payload, String questionType) {
    if (payload == null) {
      throw new IllegalStateException("AI did not return a question draft");
    }

    String normalizedType = normalize(questionType).toUpperCase();
    String stem = normalize(payload.stem());
    if (stem.isBlank()) {
      throw new IllegalStateException("AI returned an empty stem");
    }

    return switch (normalizedType) {
      case "CHOICE" -> sanitizeChoiceDraft(payload, stem);
      case "JUDGE" -> sanitizeJudgeDraft(payload, stem);
      case "BLANK" -> sanitizeBlankDraft(payload, stem);
      case "ESSAY" -> new QuestionAIDraftResponse("ESSAY", stem, List.of(), "", "", "");
      default -> throw new IllegalArgumentException("Unsupported question type");
    };
  }

  private QuestionAIDraftResponse sanitizeChoiceDraft(QuestionAIDraftResponse payload, String stem) {
    List<String> options = (payload.options() == null ? List.<String>of() : payload.options())
      .stream()
      .map(this::normalize)
      .filter(text -> !text.isBlank())
      .limit(4)
      .toList();
    if (options.size() != 4) {
      throw new IllegalStateException("AI choice draft must contain exactly 4 options");
    }

    String answerKey = normalize(payload.answerKey());
    if (answerKey.length() == 1) {
      int index = Character.toUpperCase(answerKey.charAt(0)) - 'A';
      if (index >= 0 && index < options.size()) {
        answerKey = options.get(index);
      }
    }
    if (!options.contains(answerKey)) {
      throw new IllegalStateException("AI choice draft answer does not match options");
    }

    return new QuestionAIDraftResponse("CHOICE", stem, options, answerKey, "", "");
  }

  private QuestionAIDraftResponse sanitizeJudgeDraft(QuestionAIDraftResponse payload, String stem) {
    String answerKey = normalize(payload.answerKey()).toLowerCase();
    if (!answerKey.equals("true") && !answerKey.equals("false")) {
      if (answerKey.equals("正确")) {
        answerKey = "true";
      } else if (answerKey.equals("错误")) {
        answerKey = "false";
      } else {
        throw new IllegalStateException("AI judge draft answer must be true or false");
      }
    }

    return new QuestionAIDraftResponse("JUDGE", stem, List.of("true", "false"), answerKey, "", "");
  }

  private QuestionAIDraftResponse sanitizeBlankDraft(QuestionAIDraftResponse payload, String stem) {
    String answerKey = normalize(payload.answerKey());
    if (answerKey.isBlank()) {
      throw new IllegalStateException("AI blank draft answer cannot be empty");
    }
    return new QuestionAIDraftResponse("BLANK", stem, List.of(), answerKey, "", "");
  }

  private List<CatKnowledgeInsightsResponse.MasteryPoint> calibrateMasteryScores(
    List<CatKnowledgeInsightsResponse.MasteryPoint> points,
    List<ExamAnswerDetailView> answers
  ) {
    if (points == null || points.isEmpty()) {
      return List.of();
    }

    int total = answers == null ? 0 : answers.size();
    long correctCount = answers == null ? 0 : answers.stream().filter(item -> Boolean.TRUE.equals(item.correct())).count();
    int baseScore = total == 0 ? 60 : (int) Math.round(correctCount * 100.0 / total);
    int spread = Math.min(18, Math.max(8, total == 0 ? 8 : (int) Math.round((1 - correctCount * 1.0 / total) * 20)));

    List<CatKnowledgeInsightsResponse.MasteryPoint> calibrated = new java.util.ArrayList<>();
    for (int i = 0; i < points.size(); i++) {
      CatKnowledgeInsightsResponse.MasteryPoint item = points.get(i);
      int aiScore = clampScore(item == null ? null : item.score());
      int target = clampScore(baseScore + spread / 2 - i * Math.max(4, spread / 3));
      int finalScore = clampScore((int) Math.round(aiScore * 0.35 + target * 0.65));
      calibrated.add(new CatKnowledgeInsightsResponse.MasteryPoint(item.point(), finalScore));
    }
    return calibrated;
  }

  private String extractText(MultiModalConversationResult result) {
    if (result == null || result.getOutput() == null) {
      throw new IllegalStateException("AI 返回为空");
    }

    MultiModalConversationOutput output = result.getOutput();
    if (output.getChoices() == null || output.getChoices().isEmpty()) {
      throw new IllegalStateException("AI 返回无内容");
    }

    MultiModalConversationOutput.Choice choice = output.getChoices().get(0);
    MultiModalMessage message = choice.getMessage();
    if (message == null || message.getContent() == null) {
      throw new IllegalStateException("AI 返回内容缺失");
    }

    StringBuilder builder = new StringBuilder();
    for (Map<String, Object> item : message.getContent()) {
      if (item == null) {
        continue;
      }
      Object text = item.get("text");
      if (text != null) {
        builder.append(text);
      }
    }

    String text = builder.toString().trim();
    if (text.isBlank()) {
      throw new IllegalStateException("AI 返回文本为空");
    }
    return text;
  }

  private String extractJson(String rawText) {
    int start = rawText.indexOf('{');
    int end = rawText.lastIndexOf('}');
    if (start < 0 || end <= start) {
      throw new IllegalStateException("AI 返回不是 JSON 格式");
    }
    return rawText.substring(start, end + 1);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private String compactText(String value, int maxLength) {
    String text = normalize(value).replace('\n', ' ').replace('\r', ' ');
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, Math.max(0, maxLength - 3)) + "...";
  }

  private Integer clampScore(Integer score) {
    if (score == null) {
      return 0;
    }
    return Math.max(0, Math.min(100, score));
  }
}
