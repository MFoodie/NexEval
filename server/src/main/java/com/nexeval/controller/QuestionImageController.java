package com.nexeval.controller;

import com.nexeval.repository.TeacherProfileRepository;
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
@RequestMapping("/api/question-image")
public class QuestionImageController {

  private static final long MAX_BYTES = 8L * 1024L * 1024L;

  private final Path questionImageDirectory;
  private final TeacherProfileRepository teacherProfileRepository;

  public QuestionImageController(
    @Value("${nexeval.question-image.dir:./question-images}") String questionImageDirectory,
    TeacherProfileRepository teacherProfileRepository
  ) {
    this.questionImageDirectory = Path.of(questionImageDirectory).toAbsolutePath().normalize();
    this.teacherProfileRepository = teacherProfileRepository;
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, String>> upload(
    @RequestParam("file") MultipartFile file,
    @RequestParam("teacherEid") String teacherEid
  ) throws IOException {
    String normalizedTeacherEid = normalizeTeacherEid(teacherEid);
    teacherProfileRepository.findFirstByEid(normalizedTeacherEid)
      .filter(teacher -> teacher.isCanCreateExam())
      .orElseThrow(() -> new IllegalArgumentException("当前教师没有出题权限"));

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

    Path teacherDir = questionImageDirectory.resolve(normalizedTeacherEid);
    Files.createDirectories(teacherDir);

    String filename = Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
    Path target = teacherDir.resolve(filename).normalize();
    if (!target.startsWith(teacherDir)) {
      throw new IllegalArgumentException("非法文件路径");
    }

    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

    String imagePath = "/question-images/" + normalizedTeacherEid + "/" + filename;
    return ResponseEntity.ok(Map.of("imagePath", imagePath));
  }

  private String normalizeTeacherEid(String teacherEid) {
    String normalized = teacherEid == null ? "" : teacherEid.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("teacherEid 不能为空");
    }
    if (!normalized.matches("^[A-Za-z0-9_-]{1,64}$")) {
      throw new IllegalArgumentException("teacherEid 非法");
    }
    return normalized;
  }
}
