package com.nexeval.controller;

import com.nexeval.dto.LoginResponse;
import com.nexeval.service.UserAuthService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/avatar")
public class AvatarController {

  private static final long MAX_BYTES = 5L * 1024L * 1024L;

  private final UserAuthService userAuthService;
  private final Path avatarDirectory;

  public AvatarController(
    UserAuthService userAuthService,
    @Value("${nexeval.avatar.dir:./avatar}") String avatarDirectory
  ) {
    this.userAuthService = userAuthService;
    this.avatarDirectory = Path.of(avatarDirectory).toAbsolutePath().normalize();
  }

  @PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<LoginResponse> uploadAvatar(@PathVariable String userId, @RequestParam("file") MultipartFile file)
    throws IOException {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("头像文件不能为空");
    }

    if (file.getSize() > MAX_BYTES) {
      throw new IllegalArgumentException("头像文件不能超过 5MB");
    }

    String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "";
    if (!("image/jpeg".equalsIgnoreCase(contentType) || "image/png".equalsIgnoreCase(contentType))) {
      throw new IllegalArgumentException("头像仅支持 JPG 或 PNG");
    }

    Files.createDirectories(avatarDirectory);
    Path target = avatarDirectory.resolve(userId + ".png");
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

    return ResponseEntity.ok(userAuthService.getProfile(userId));
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<LoginResponse> restoreDefaultAvatar(@PathVariable String userId) throws IOException {
    Path target = avatarDirectory.resolve(userId + ".png");
    Files.deleteIfExists(target);
    return ResponseEntity.ok(userAuthService.getProfile(userId));
  }
}
