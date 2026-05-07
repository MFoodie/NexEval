package com.nexeval.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/answer-image")
public class EssayAnswerImageController {

  private static final long MAX_BYTES = 8L * 1024L * 1024L;

  private final Path answerImageDirectory;

  public EssayAnswerImageController(@Value("${nexeval.answer-image.dir:./answer-images}") String answerImageDirectory) {
    this.answerImageDirectory = Path.of(answerImageDirectory).toAbsolutePath().normalize();
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, String>> upload(
    @RequestParam("file") MultipartFile file,
    @RequestParam("sessionId") String sessionId
  ) throws IOException {
    String normalizedSessionId = normalizeSessionId(sessionId);
    if (file.isEmpty()) {
      throw new IllegalArgumentException("图片文件不能为空");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new IllegalArgumentException("图片不能超过 8MB");
    }

    String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase() : "";
    if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType) || "image/webp".equals(contentType))) {
      throw new IllegalArgumentException("仅支持 JPG/PNG/WEBP 图片");
    }

    String extension = switch (contentType) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };

    Path sessionDir = answerImageDirectory.resolve(normalizedSessionId);
    Files.createDirectories(sessionDir);

    String filename = Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
    Path target = sessionDir.resolve(filename).normalize();

    if (!target.startsWith(sessionDir)) {
      throw new IllegalArgumentException("非法文件路径");
    }

    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

    String imagePath = "/answer-images/" + normalizedSessionId + "/" + filename;
    return ResponseEntity.ok(Map.of("imagePath", imagePath));
  }

  private String normalizeSessionId(String sessionId) {
    String normalized = sessionId == null ? "" : sessionId.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("sessionId 不能为空");
    }
    if (!normalized.matches("^[A-Za-z0-9_-]{1,64}$")) {
      throw new IllegalArgumentException("sessionId 非法");
    }
    return normalized;
  }
}
