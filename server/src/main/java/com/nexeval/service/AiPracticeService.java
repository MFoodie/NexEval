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