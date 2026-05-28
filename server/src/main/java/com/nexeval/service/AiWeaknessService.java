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
}
