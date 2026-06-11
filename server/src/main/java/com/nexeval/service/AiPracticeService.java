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
import com.nexeval.dto.ExamAnswerDetailView;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiPracticeService {

  private static final Logger log = LoggerFactory.getLogger(AiPracticeService.class);

  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public AiPracticeService(
    ObjectMapper objectMapper,
    @Value("${dashscope.api-key:}") String apiKey,
    @Value("${dashscope.model:qwen-plus}") String model
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null ? "qwen-plus" : model.trim();
  }

  public List<String> selectPracticeQuestionIds(
    String courseNo,
    String difficultyLabel,
    int questionCount,
    List<PracticeQuestionCandidate> candidates
  ) {
    if (apiKey.isBlank() || candidates == null || candidates.isEmpty() || questionCount <= 0) {
      return List.of();
    }

    String prompt = buildPrompt(courseNo, difficultyLabel, questionCount, candidates);
    MultiModalMessage systemMessage = MultiModalMessage.builder()
      .role(Role.SYSTEM.getValue())
      .content(List.of(Map.of("text", "你是在线考试系统的组卷助手，必须严格按照要求返回 JSON。")))
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
      String rawText = extractText(result);
      PracticeSelectionPayload payload = parsePayload(rawText);
      if (payload == null || payload.questionIds() == null || payload.questionIds().isEmpty()) {
        return List.of();
      }
      return payload.questionIds();
    } catch (ApiException | NoApiKeyException | UploadFileException ex) {
      log.warn("AI practice selection failed: {}", ex.getMessage());
      return List.of();
    }
  }

  public String generateCatSummary(
    String courseNo,
    String courseName,
    int estimatedScore,
    int systemPrecision,
    int answeredCount,
    int maxQuestions,
    List<ExamAnswerDetailView> answers
  ) {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key 未配置");
    }

    String prompt = buildCatSummaryPrompt(
      courseNo,
      courseName,
      estimatedScore,
      systemPrecision,
      answeredCount,
      maxQuestions,
      answers
    );

    MultiModalMessage systemMessage = MultiModalMessage.builder()
      .role(Role.SYSTEM.getValue())
      .content(List.of(Map.of("text", "你是严谨的学习诊断顾问，输出中文专业诊断文本。")))
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
      log.warn("AI cat summary generation failed: {}", ex.getMessage());
      throw new IllegalStateException("AI 简报生成失败，请稍后重试");
    }
  }

  private String buildPrompt(
    String courseNo,
    String difficultyLabel,
    int questionCount,
    List<PracticeQuestionCandidate> candidates
  ) {
    StringBuilder builder = new StringBuilder();
    builder.append("课程编号：").append(normalize(courseNo)).append('\n');
    builder.append("目标难度：").append(normalize(difficultyLabel)).append('\n');
    builder.append("题目数量：").append(questionCount).append('\n');
    builder.append("候选题目如下（仅能从中选择，不要自造题目，不要重复）：\n");
    for (int i = 0; i < candidates.size(); i++) {
      PracticeQuestionCandidate candidate = candidates.get(i);
      builder.append(i + 1)
        .append(". id=").append(normalize(candidate.id()))
        .append(", type=").append(normalize(candidate.type()))
        .append(", difficulty=").append(candidate.difficulty())
        .append(", points=").append(candidate.points())
        .append(", stem=").append(normalize(candidate.stem()))
        .append('\n');
    }
    builder.append("请返回 JSON，格式如下：{\"questionIds\":[\"id1\",\"id2\"]}。\n");
    builder.append("要求：\n");
    builder.append("1. 必须返回尽量接近题目数量的题目 id，顺序就是出题顺序。\n");
    builder.append("2. 题目应符合目标难度。\n");
    builder.append("3. 尽量覆盖单选、判断、填空和大题。\n");
    builder.append("4. 只输出 JSON，不要解释。\n");
    return builder.toString();
  }

  private String buildCatSummaryPrompt(
    String courseNo,
    String courseName,
    int estimatedScore,
    int systemPrecision,
    int answeredCount,
    int maxQuestions,
    List<ExamAnswerDetailView> answers
  ) {
    List<ExamAnswerDetailView> safeAnswers = answers == null ? List.of() : answers;
    long correctCount = safeAnswers.stream().filter(item -> Boolean.TRUE.equals(item.correct())).count();
    int correctRate = safeAnswers.isEmpty()
      ? 0
      : (int) Math.round(correctCount * 100.0 / safeAnswers.size());
    StringBuilder builder = new StringBuilder();
    builder.append("请严格基于以下 CAT 本次作答记录，生成一段 180~260 字的中文学习诊断报告。\n");
    builder.append("课程编号：").append(normalize(courseNo)).append('\n');
    builder.append("课程名称：").append(normalize(courseName)).append('\n');
    builder.append("实际答题数：").append(answeredCount).append('/').append(maxQuestions).append('\n');
    builder.append("实际答对数：").append(correctCount).append('\n');
    builder.append("实际正确率：").append(correctRate).append("%\n");
    builder.append("IRT 能力估计（不是正确率）：").append(estimatedScore).append("%\n");
    builder.append("能力估计稳定度（不是正确率）：").append(systemPrecision).append("%\n");
    builder.append("逐题证据（题号只以这里的序号为准）：\n");
    for (int i = 0; i < safeAnswers.size(); i++) {
      ExamAnswerDetailView answer = safeAnswers.get(i);
      builder.append(i + 1)
        .append(". 题干=").append(compact(answer.stem(), 160))
        .append("; 学生答案=").append(compact(answer.answerText(), 60))
        .append("; 正确答案=").append(compact(answer.correctAnswer(), 60))
        .append("; 判定=").append(Boolean.TRUE.equals(answer.correct()) ? "正确" : "错误")
        .append("; 难度=").append(answer.difficulty() == null ? "未知" : answer.difficulty())
        .append('\n');
    }
    builder.append("写作要求：\n");
    builder.append("1) 只能使用上述数据，不得编造题目、题号、答案、知识点或作答表现。\n");
    builder.append("2) 不得引用大于实际答题数的题号；提到某题时必须与对应题干和判定一致。\n");
    builder.append("3) 优势与薄弱点必须有逐题证据；证据不足时应明确说明样本有限，不得强行凑数。\n");
    builder.append("4) 给出与错题内容直接相关、可执行的下一步训练建议。\n");
    builder.append("5) 只输出纯文本，不要 Markdown，不要项目符号。\n");
    return builder.toString();
  }

  private String compact(String value, int maxLength) {
    String normalized = normalize(value).replaceAll("\\s+", " ");
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, maxLength) + "...";
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

  private PracticeSelectionPayload parsePayload(String rawText) {
    String jsonText = extractJson(rawText);
    try {
      return objectMapper.readValue(jsonText, PracticeSelectionPayload.class);
    } catch (IOException ex) {
      throw new IllegalStateException("AI 返回不是有效 JSON");
    }
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

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PracticeQuestionCandidate(String id, String type, double difficulty, int points, String stem) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PracticeSelectionPayload(List<String> questionIds) {}
}
