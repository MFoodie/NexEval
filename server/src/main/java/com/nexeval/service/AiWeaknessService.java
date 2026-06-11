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
import com.nexeval.dto.WeaknessEvaluateRequest;
import com.nexeval.dto.WeaknessEvaluateResponse;
import com.nexeval.dto.WeaknessExplainRequest;
import com.nexeval.dto.WeaknessExplainResponse;
import com.nexeval.dto.WeaknessQuestionRequest;
import com.nexeval.dto.WeaknessQuestionResponse;
import com.nexeval.dto.WeaknessTrainingRequest;
import com.nexeval.dto.WeaknessTrainingResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiWeaknessService {

  private static final Logger log = LoggerFactory.getLogger(AiWeaknessService.class);
  private static final Pattern KNOWLEDGE_POINT_PATTERN =
    Pattern.compile("【\\s*知识点\\s*[：:]\\s*([^】]+)】");

  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final long timeoutMillis;

  public AiWeaknessService(
    ObjectMapper objectMapper,
    @Value("${dashscope.api-key:}") String apiKey,
    @Value("${dashscope.model:qwen-plus}") String model,
    @Value("${dashscope.timeout-millis:45000}") long timeoutMillis
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null ? "qwen-plus" : model.trim();
    this.timeoutMillis = timeoutMillis <= 0 ? 45000L : timeoutMillis;
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
    List<ExamAnswerDetailView> safeAnswers = answers == null ? List.of() : answers;
    if (safeAnswers.isEmpty()) {
      return new CatKnowledgeInsightsResponse(List.of(), List.of());
    }

    CatKnowledgeInsightsResponse measured = calculateLabeledKnowledgeInsights(safeAnswers);
    if (!measured.masteryPoints().isEmpty()) {
      return measured;
    }

    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key 未配置");
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

  public WeaknessTrainingResponse generateTraining(WeaknessTrainingRequest request) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key unavailable");
    }
    if (request == null) {
      throw new IllegalArgumentException("request cannot be null");
    }

    String prompt = buildTrainingPrompt(request);
    String rawText = callDashscope(
      prompt,
      "You are a weakness-training question generator. Return strict JSON only.",
      "AI weakness training generation failed"
    );
    String jsonText = extractJson(rawText);

    try {
      WeaknessTrainingResponse payload = objectMapper.readValue(jsonText, WeaknessTrainingResponse.class);
      return sanitizeTrainingResponse(payload, request);
    } catch (IOException ex) {
      throw new IllegalStateException("AI training questions are not valid JSON");
    }
  }

  public WeaknessEvaluateResponse evaluateAnswer(WeaknessEvaluateRequest request) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key unavailable");
    }
    if (request == null || request.question() == null) {
      throw new IllegalArgumentException("question cannot be null");
    }

    String prompt = buildEvaluatePrompt(request);
    String rawText = callDashscope(
      prompt,
      "You are an answer evaluation assistant. Return strict JSON only.",
      "AI answer evaluation failed"
    );
    String jsonText = extractJson(rawText);

    try {
      WeaknessEvaluateResponse payload = objectMapper.readValue(jsonText, WeaknessEvaluateResponse.class);
      return sanitizeEvaluateResponse(payload, request);
    } catch (IOException ex) {
      throw new IllegalStateException("AI evaluation is not valid JSON");
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
      CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        try {
          MultiModalConversation conversation = new MultiModalConversation();
          MultiModalConversationResult result = conversation.call(param);
          return extractText(result);
        } catch (ApiException | NoApiKeyException | UploadFileException ex) {
          throw new CompletionException(ex);
        }
      });
      return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause() instanceof CompletionException completionEx
        ? completionEx.getCause()
        : ex.getCause();
      if (cause instanceof TimeoutException) {
        log.warn("AI weakness generation timed out after {} ms with model {}", timeoutMillis, model);
        throw new IllegalStateException("AI 生成超时，请稍后重试");
      }
      if (cause instanceof ApiException || cause instanceof NoApiKeyException || cause instanceof UploadFileException) {
        log.warn("AI weakness generation failed with model {}: {}", model, cause.getMessage());
        throw new IllegalStateException(errorMessage);
      }
      throw new IllegalStateException(errorMessage, ex);
    } catch (TimeoutException ex) {
      log.warn("AI weakness generation timed out after {} ms with model {}", timeoutMillis, model);
      throw new IllegalStateException("AI 生成超时，请稍后重试");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("AI generation interrupted, please retry");
    } catch (RuntimeException ex) {
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

  private String buildTrainingPrompt(WeaknessTrainingRequest request) {
    int choiceCount = positiveOrDefault(request.choiceCount(), 3);
    int judgeCount = positiveOrDefault(request.judgeCount(), 3);
    int blankCount = positiveOrDefault(request.blankCount(), 3);
    int essayCount = positiveOrDefault(request.essayCount(), 1);
    StringBuilder builder = new StringBuilder();
    builder.append("Generate a weakness-targeted training set for one student.\n");
    builder.append("Course No: ").append(normalize(request.courseNo())).append('\n');
    builder.append("Course Name: ").append(normalize(request.courseName())).append('\n');
    builder.append("Target counts: ")
      .append(choiceCount).append(" choice, ")
      .append(judgeCount).append(" judge, ")
      .append(blankCount).append(" blank, ")
      .append(essayCount).append(" essay.\n");

    List<String> weakPoints = request.weakPoints() == null ? List.of() : request.weakPoints().stream()
      .map(this::normalize)
      .filter(text -> !text.isBlank())
      .distinct()
      .toList();
    if (!weakPoints.isEmpty()) {
      builder.append("Weak knowledge points: ").append(String.join(" | ", weakPoints)).append('\n');
    }

    List<WeaknessQuestionRequest.WeaknessQuestionHistory> history =
      request.history() == null ? List.of() : request.history();
    if (!history.isEmpty()) {
      builder.append("Wrong-answer evidence:\n");
      int limit = Math.min(history.size(), 10);
      for (int i = 0; i < limit; i++) {
        WeaknessQuestionRequest.WeaknessQuestionHistory item = history.get(i);
        builder.append(i + 1)
          .append(". stem=").append(compactText(item.stem(), 120))
          .append("; userAnswer=").append(compactText(item.userAnswer(), 40))
          .append("; correctAnswer=").append(compactText(item.correctAnswer(), 40))
          .append('\n');
      }
    }

    builder.append("Return JSON only in this format:\n");
    builder.append("{\"questions\":[")
      .append("{\"id\":\"q1\",\"type\":\"choice\",\"stem\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswer\":\"...\",\"knowledgePoints\":[\"...\"],\"imagePath\":\"\",\"explanation\":\"...\"}")
      .append("]}\n");
    builder.append("Requirements:\n");
    builder.append("1. Generate exactly the requested counts and keep the order: all choice first, then judge, then blank, then essay.\n");
    builder.append("2. CHOICE must have exactly 4 options and one correctAnswer matching one option or one option letter.\n");
    builder.append("3. JUDGE must use options [\"true\",\"false\"] and correctAnswer true or false.\n");
    builder.append("4. BLANK must have empty options and a concise correctAnswer.\n");
    builder.append("5. ESSAY must have empty options and a non-empty reference answer in correctAnswer so the answer can be evaluated later.\n");
    builder.append("6. Every question must target the weak points or clearly relate to the wrong-answer evidence.\n");
    builder.append("7. knowledgePoints must be a non-empty string array.\n");
    builder.append("8. No markdown, no commentary, JSON only.\n");
    return builder.toString();
  }

  private String buildEvaluatePrompt(WeaknessEvaluateRequest request) {
    WeaknessEvaluateRequest.WeaknessEvaluateQuestion question = request.question();
    StringBuilder builder = new StringBuilder();
    builder.append("Evaluate the student's answer and return strict JSON only.\n");
    builder.append("Question type: ").append(normalize(question.type())).append('\n');
    builder.append("Stem: ").append(compactText(question.stem(), 220)).append('\n');
    if (question.options() != null && !question.options().isEmpty()) {
      builder.append("Options: ").append(String.join(" | ", question.options())).append('\n');
    }
    builder.append("Reference answer: ").append(compactText(question.correctAnswer(), 220)).append('\n');
    builder.append("Student answer: ").append(compactText(request.userAnswer(), 220)).append('\n');
    builder.append("Return JSON only in this format:\n");
    builder.append("{\"correct\":true,\"score\":100,\"correctAnswer\":\"...\",\"explanation\":\"...\"}\n");
    builder.append("Requirements:\n");
    builder.append("1. correct must be a boolean.\n");
    builder.append("2. score must be an integer from 0 to 100.\n");
    builder.append("3. correctAnswer must repeat the reference answer clearly.\n");
    builder.append("4. explanation must be concise and explain why the answer is right or wrong.\n");
    builder.append("5. No markdown, JSON only.\n");
    return builder.toString();
  }

  CatKnowledgeInsightsResponse sanitizeCatKnowledgeInsights(
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
    boolean hasCorrectAnswer = answers != null
      && answers.stream().anyMatch(answer -> answer != null && Boolean.TRUE.equals(answer.correct()));

    for (CatKnowledgeInsightsResponse.MasteryPoint item : rawMastery) {
      String point = normalize(item == null ? null : item.point());
      if (point.isBlank() || !masterySeen.add(point)) {
        continue;
      }
      masteryPoints.add(new CatKnowledgeInsightsResponse.MasteryPoint(
        point,
        hasCorrectAnswer ? clampScore(item == null ? null : item.score()) : 0,
        null,
        null,
        true
      ));
      if (masteryPoints.size() >= 4) {
        break;
      }
    }

    masteryPoints.sort((left, right) -> Integer.compare(
      clampScore(right == null ? null : right.score()),
      clampScore(left == null ? null : left.score())
    ));
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

  private CatKnowledgeInsightsResponse calculateLabeledKnowledgeInsights(
    List<ExamAnswerDetailView> answers
  ) {
    Map<String, KnowledgePointStats> statsByPoint = new LinkedHashMap<>();
    for (ExamAnswerDetailView answer : answers) {
      String point = extractKnowledgePoint(answer == null ? null : answer.stem());
      if (point.isBlank()) {
        continue;
      }
      KnowledgePointStats stats = statsByPoint.computeIfAbsent(point, ignored -> new KnowledgePointStats());
      stats.questionCount += 1;
      if (Boolean.TRUE.equals(answer.correct())) {
        stats.correctCount += 1;
      }
    }

    List<CatKnowledgeInsightsResponse.MasteryPoint> masteryPoints = statsByPoint.entrySet().stream()
      .map(entry -> {
        KnowledgePointStats stats = entry.getValue();
        int score = stats.questionCount == 0
          ? 0
          : (int) Math.round(stats.correctCount * 100.0 / stats.questionCount);
        return new CatKnowledgeInsightsResponse.MasteryPoint(
          entry.getKey(),
          score,
          stats.correctCount,
          stats.questionCount,
          false
        );
      })
      .sorted(
        Comparator.comparingInt((CatKnowledgeInsightsResponse.MasteryPoint item) -> item.questionCount())
          .reversed()
          .thenComparingInt(item -> item.score())
          .thenComparing(CatKnowledgeInsightsResponse.MasteryPoint::point)
      )
      .limit(4)
      .toList();

    List<String> weakPoints = masteryPoints.stream()
      .filter(item -> item.score() < 60)
      .sorted(Comparator.comparingInt(CatKnowledgeInsightsResponse.MasteryPoint::score))
      .map(CatKnowledgeInsightsResponse.MasteryPoint::point)
      .limit(3)
      .toList();

    return new CatKnowledgeInsightsResponse(masteryPoints, weakPoints);
  }

  private String extractKnowledgePoint(String stem) {
    Matcher matcher = KNOWLEDGE_POINT_PATTERN.matcher(normalize(stem));
    if (!matcher.find()) {
      return "";
    }
    return normalize(matcher.group(1));
  }

  private static final class KnowledgePointStats {
    private int correctCount;
    private int questionCount;
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

  private WeaknessTrainingResponse sanitizeTrainingResponse(
    WeaknessTrainingResponse payload,
    WeaknessTrainingRequest request
  ) {
    List<WeaknessQuestionResponse> rawQuestions = payload == null || payload.questions() == null
      ? List.of()
      : payload.questions();
    List<WeaknessQuestionResponse> questions = new ArrayList<>();
    int choiceCount = positiveOrDefault(request.choiceCount(), 3);
    int judgeCount = positiveOrDefault(request.judgeCount(), 3);
    int blankCount = positiveOrDefault(request.blankCount(), 3);
    int essayCount = positiveOrDefault(request.essayCount(), 1);
    int total = choiceCount + judgeCount + blankCount + essayCount;

    for (WeaknessQuestionResponse item : rawQuestions) {
      WeaknessQuestionResponse normalized = sanitizeTrainingQuestion(item);
      if (normalized != null) {
        questions.add(normalized);
      }
      if (questions.size() >= total) {
        break;
      }
    }

    if (questions.size() != total) {
      throw new IllegalStateException("AI did not return the required number of training questions");
    }

    ensureTypeCount(questions, "choice", choiceCount);
    ensureTypeCount(questions, "judge", judgeCount);
    ensureTypeCount(questions, "blank", blankCount);
    ensureTypeCount(questions, "essay", essayCount);
    return new WeaknessTrainingResponse(questions);
  }

  private WeaknessQuestionResponse sanitizeTrainingQuestion(WeaknessQuestionResponse item) {
    if (item == null) {
      return null;
    }
    String type = normalizeQuestionType(item.type());
    String stem = normalize(item.stem());
    if (stem.isBlank() || type.isBlank()) {
      return null;
    }

    List<String> knowledgePoints = item.knowledgePoints() == null ? List.of() : item.knowledgePoints().stream()
      .map(this::normalize)
      .filter(text -> !text.isBlank())
      .distinct()
      .toList();
    if (knowledgePoints.isEmpty()) {
      knowledgePoints = List.of("薄弱知识点强化");
    }

    return switch (type) {
      case "choice" -> sanitizeChoiceTrainingQuestion(item, stem, knowledgePoints);
      case "judge" -> sanitizeJudgeTrainingQuestion(item, stem, knowledgePoints);
      case "blank" -> sanitizeBlankTrainingQuestion(item, stem, knowledgePoints);
      case "essay" -> sanitizeEssayTrainingQuestion(item, stem, knowledgePoints);
      default -> null;
    };
  }

  private WeaknessQuestionResponse sanitizeChoiceTrainingQuestion(
    WeaknessQuestionResponse item,
    String stem,
    List<String> knowledgePoints
  ) {
    List<String> options = item.options() == null ? List.of() : item.options().stream()
      .map(this::normalize)
      .filter(text -> !text.isBlank())
      .limit(4)
      .toList();
    if (options.size() != 4) {
      return null;
    }

    String answer = normalize(item.correctAnswer());
    if (answer.length() == 1) {
      int index = Character.toUpperCase(answer.charAt(0)) - 'A';
      if (index >= 0 && index < options.size()) {
        answer = options.get(index);
      }
    }
    if (!options.contains(answer)) {
      return null;
    }

    return new WeaknessQuestionResponse(
      normalize(item.id()).isBlank() ? generateTempId("choice") : normalize(item.id()),
      "choice",
      stem,
      options,
      answer,
      knowledgePoints,
      normalize(item.imagePath()),
      normalize(item.explanation())
    );
  }

  private WeaknessQuestionResponse sanitizeJudgeTrainingQuestion(
    WeaknessQuestionResponse item,
    String stem,
    List<String> knowledgePoints
  ) {
    String answer = normalize(item.correctAnswer()).toLowerCase();
    if (answer.equals("正确")) answer = "true";
    if (answer.equals("错误")) answer = "false";
    if (!answer.equals("true") && !answer.equals("false")) {
      return null;
    }

    return new WeaknessQuestionResponse(
      normalize(item.id()).isBlank() ? generateTempId("judge") : normalize(item.id()),
      "judge",
      stem,
      List.of("true", "false"),
      answer,
      knowledgePoints,
      normalize(item.imagePath()),
      normalize(item.explanation())
    );
  }

  private WeaknessQuestionResponse sanitizeBlankTrainingQuestion(
    WeaknessQuestionResponse item,
    String stem,
    List<String> knowledgePoints
  ) {
    String answer = normalize(item.correctAnswer());
    if (answer.isBlank()) {
      return null;
    }
    return new WeaknessQuestionResponse(
      normalize(item.id()).isBlank() ? generateTempId("blank") : normalize(item.id()),
      "blank",
      stem,
      List.of(),
      answer,
      knowledgePoints,
      normalize(item.imagePath()),
      normalize(item.explanation())
    );
  }

  private WeaknessQuestionResponse sanitizeEssayTrainingQuestion(
    WeaknessQuestionResponse item,
    String stem,
    List<String> knowledgePoints
  ) {
    String answer = normalize(item.correctAnswer());
    if (answer.isBlank()) {
      return null;
    }
    return new WeaknessQuestionResponse(
      normalize(item.id()).isBlank() ? generateTempId("essay") : normalize(item.id()),
      "essay",
      stem,
      List.of(),
      answer,
      knowledgePoints,
      normalize(item.imagePath()),
      normalize(item.explanation())
    );
  }

  private WeaknessEvaluateResponse sanitizeEvaluateResponse(
    WeaknessEvaluateResponse payload,
    WeaknessEvaluateRequest request
  ) {
    String type = normalizeQuestionType(request.question().type());
    String correctAnswer = normalize(request.question().correctAnswer());
    String userAnswer = normalize(request.userAnswer());
    boolean objective = type.equals("choice") || type.equals("judge") || type.equals("blank");

    boolean correct;
    int score;
    if (objective) {
      correct = matchesObjectiveAnswer(type, correctAnswer, userAnswer);
      score = correct ? 100 : 0;
    } else {
      correct = payload != null && Boolean.TRUE.equals(payload.correct());
      score = clampScore(payload == null ? null : payload.score());
    }

    String explanation = normalize(payload == null ? null : payload.explanation());
    if (explanation.isBlank()) {
      explanation = correct ? "回答正确，继续保持。" : "回答不正确，请对照参考答案继续巩固。";
    }

    return new WeaknessEvaluateResponse(
      correct,
      score,
      correctAnswer,
      explanation
    );
  }

  private boolean matchesObjectiveAnswer(String type, String correctAnswer, String userAnswer) {
    if (type.equals("judge")) {
      String normalizedCorrect = correctAnswer.toLowerCase();
      String normalizedUser = userAnswer.toLowerCase();
      if (normalizedUser.equals("正确")) normalizedUser = "true";
      if (normalizedUser.equals("错误")) normalizedUser = "false";
      return normalizedCorrect.equals(normalizedUser);
    }
    return correctAnswer.equalsIgnoreCase(userAnswer);
  }

  private void ensureTypeCount(List<WeaknessQuestionResponse> questions, String type, int expected) {
    long count = questions.stream().filter(item -> type.equals(normalizeQuestionType(item.type()))).count();
    if (count != expected) {
      throw new IllegalStateException("AI returned unexpected question type distribution");
    }
  }

  private String normalizeQuestionType(String value) {
    return normalize(value).toLowerCase();
  }

  private String generateTempId(String prefix) {
    return prefix + "-" + System.nanoTime();
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

  private int positiveOrDefault(Integer value, int fallback) {
    return value == null || value <= 0 ? fallback : value;
  }

  private Integer clampScore(Integer score) {
    if (score == null) {
      return 0;
    }
    return Math.max(0, Math.min(100, score));
  }
}
