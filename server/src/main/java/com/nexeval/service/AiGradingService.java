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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiGradingService {

  private static final Logger log = LoggerFactory.getLogger(AiGradingService.class);

  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final double confidenceThreshold;
  private final Path answerImageDirectory;
  private final Path questionImageDirectory;
  private final Path figImageDirectory;

  public AiGradingService(
    ObjectMapper objectMapper,
    @Value("${dashscope.api-key:}") String apiKey,
    @Value("${dashscope.model:qwen-vl-flash}") String model,
    @Value("${dashscope.confidence-threshold:0.8}") double confidenceThreshold,
    @Value("${nexeval.answer-image.dir:./answer-images}") String answerImageDirectory,
    @Value("${nexeval.question-image.dir:./question-images}") String questionImageDirectory,
    @Value("${nexeval.fig.dir:./fig}") String figImageDirectory
  ) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null ? "qwen-vl-flash" : model.trim();
    this.confidenceThreshold = confidenceThreshold;
    this.answerImageDirectory = Path.of(answerImageDirectory).toAbsolutePath().normalize();
    this.questionImageDirectory = Path.of(questionImageDirectory).toAbsolutePath().normalize();
    this.figImageDirectory = Path.of(figImageDirectory).toAbsolutePath().normalize();
  }

  public double getConfidenceThreshold() {
    return confidenceThreshold;
  }

  public AiGradeResult gradeEssay(
    String questionStem,
    int maxScore,
    String standardAnswer,
    String scoringRubric,
    String studentText,
    String answerImagePath,
    String questionImagePath
  ) {
    ensureApiKeyConfigured();

    String prompt = buildPrompt(
      normalize(questionStem),
      maxScore,
      normalize(standardAnswer),
      normalize(scoringRubric),
      normalize(studentText)
    );

    List<Map<String, Object>> userContent = new ArrayList<>();
    if (!normalize(studentText).isBlank()) {
      userContent.add(Collections.singletonMap("text", normalize(studentText)));
    }

    String questionImageUrl = resolveImageDataUrl(questionImagePath);
    if (questionImageUrl != null) {
      userContent.add(Collections.singletonMap("image", questionImageUrl));
    }

    String answerImageUrl = resolveImageDataUrl(answerImagePath);
    if (answerImageUrl != null) {
      userContent.add(Collections.singletonMap("image", answerImageUrl));
    }

    MultiModalMessage systemMessage = MultiModalMessage.builder()
      .role(Role.SYSTEM.getValue())
      .content(Collections.singletonList(Collections.singletonMap("text", prompt)))
      .build();

    MultiModalMessage userMessage = MultiModalMessage.builder()
      .role(Role.USER.getValue())
      .content(userContent.isEmpty()
        ? Collections.singletonList(Collections.singletonMap("text", "学生未提供文字作答，仅提供图片。"))
        : userContent)
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
      AiScorePayload payload = parseAiPayload(rawText);
      int normalizedScore = clampScore(payload.score(), maxScore);
      double normalizedConfidence = clampConfidence(payload.confidence());
      String comment = normalize(payload.comment());
      String aiLog = buildAiLogJson(payload);
      return new AiGradeResult(normalizedScore, comment, normalizedConfidence, aiLog);
    } catch (ApiException | NoApiKeyException | UploadFileException ex) {
      log.warn("AI grading failed: {}", ex.getMessage());
      throw new IllegalStateException("AI 批改失败，请稍后重试");
    }
  }

  private void ensureApiKeyConfigured() {
    if (apiKey.isBlank()) {
      throw new IllegalStateException("DashScope API Key 未配置");
    }
  }

  private String buildPrompt(
    String questionStem,
    int maxScore,
    String standardAnswer,
    String scoringRubric,
    String studentText
  ) {
    StringBuilder builder = new StringBuilder();
    builder.append("你是一位严谨的高校教师，请严格按照提供的标准打分，不偏袒、不随意扣分。\n");
    builder.append("[题目题干]：").append(questionStem).append("\n");
    builder.append("[题目满分]：").append(maxScore).append(" 分\n");
    builder.append("[标准答案]：").append(standardAnswer.isBlank() ? "无" : standardAnswer).append("\n");
    builder.append("[评分细则/踩分点]：").append(scoringRubric.isBlank() ? "无" : scoringRubric).append("\n");
    builder.append("[学生作答内容]：").append(studentText.isBlank() ? "无" : studentText).append("\n");
    builder.append("输出约束：请仅输出符合上述定义的 JSON 格式，不要包含任何 Markdown 标记或多余的解释。\n");
    builder.append("JSON 格式：{\"score\": 数字, \"comment\": 字符串, \"confidence\": 浮点数}\n");
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

  private AiScorePayload parseAiPayload(String rawText) {
    String jsonText = extractJson(rawText);
    try {
      AiScorePayload payload = objectMapper.readValue(jsonText, AiScorePayload.class);
      if (payload == null) {
        throw new IllegalStateException("AI 返回解析失败");
      }
      return payload;
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

  private String buildAiLogJson(AiScorePayload payload) {
    Map<String, Object> logMap = new LinkedHashMap<>();
    logMap.put("score", payload.score());
    logMap.put("comment", normalize(payload.comment()));
    logMap.put("confidence", payload.confidence());

    String json = toJson(logMap);
    if (json.length() <= 255) {
      return json;
    }

    String comment = normalize(payload.comment());
    int low = 0;
    int high = comment.length();
    String best = "";

    while (low <= high) {
      int mid = (low + high) / 2;
      String candidate = comment.substring(0, mid);
      logMap.put("comment", candidate);
      json = toJson(logMap);
      if (json.length() <= 255) {
        best = candidate;
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    logMap.put("comment", best);
    return toJson(logMap);
  }

  private String toJson(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException ex) {
      return "{}";
    }
  }

  private String resolveImageDataUrl(String imagePath) {
    String normalized = normalize(imagePath);
    if (normalized.isBlank()) {
      return null;
    }

    if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
      return normalized;
    }

    return resolveLocalImageDataUrl(normalized);
  }

  private String resolveLocalImageDataUrl(String normalizedPath) {
    Path baseDir = null;
    String relative = normalizedPath;

    if (normalizedPath.startsWith("/answer-images/")) {
      baseDir = answerImageDirectory;
      relative = normalizedPath.substring("/answer-images/".length());
    } else if (normalizedPath.startsWith("answer-images/")) {
      baseDir = answerImageDirectory;
      relative = normalizedPath.substring("answer-images/".length());
    } else if (normalizedPath.startsWith("/question-images/")) {
      baseDir = questionImageDirectory;
      relative = normalizedPath.substring("/question-images/".length());
    } else if (normalizedPath.startsWith("question-images/")) {
      baseDir = questionImageDirectory;
      relative = normalizedPath.substring("question-images/".length());
    } else if (normalizedPath.startsWith("/fig/")) {
      baseDir = figImageDirectory;
      relative = normalizedPath.substring("/fig/".length());
    } else if (normalizedPath.startsWith("fig/")) {
      baseDir = figImageDirectory;
      relative = normalizedPath.substring("fig/".length());
    }

    if (baseDir == null) {
      return null;
    }

    Path target = baseDir.resolve(relative).normalize();
    if (!target.startsWith(baseDir)) {
      throw new IllegalArgumentException("非法图片路径");
    }

    try {
      byte[] bytes = Files.readAllBytes(target);
      String mime = detectMimeType(target, bytes);
      String base64 = Base64.getEncoder().encodeToString(bytes);
      return "data:" + mime + ";base64," + base64;
    } catch (IOException ex) {
      log.warn("Failed to read image: {}", ex.getMessage());
      return null;
    }
  }

  private String detectMimeType(Path file, byte[] bytes) {
    try {
      String mime = Files.probeContentType(file);
      if (mime != null && !mime.isBlank()) {
        return mime.toLowerCase(Locale.ROOT);
      }
    } catch (IOException ignored) {
    }

    if (bytes.length >= 2) {
      if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
        return "image/jpeg";
      }
      if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50) {
        return "image/png";
      }
      if ((bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49) {
        return "image/webp";
      }
    }

    return "image/jpeg";
  }

  private int clampScore(Integer value, int maxScore) {
    int score = value == null ? 0 : value;
    if (score < 0) {
      score = 0;
    }
    if (maxScore > 0 && score > maxScore) {
      score = maxScore;
    }
    return score;
  }

  private double clampConfidence(Double value) {
    if (value == null) {
      return 0.0;
    }
    if (value < 0.0) {
      return 0.0;
    }
    if (value > 1.0) {
      return 1.0;
    }
    return value;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record AiScorePayload(Integer score, String comment, Double confidence) {
  }
}
