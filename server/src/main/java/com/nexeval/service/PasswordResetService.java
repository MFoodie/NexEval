package com.nexeval.service;

import com.nexeval.model.UserAccount;
import com.nexeval.repository.UserAccountRepository;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

  private static final String TOKEN_PREFIX = "pwd-reset:";
  private static final String RATE_PREFIX = "rate:";

  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final StringRedisTemplate redis;
  private final Duration tokenTtl;
  private final Duration rateLimitTtl;
  private final String resetPageUrl;

  public PasswordResetService(
    UserAccountRepository userAccountRepository,
    PasswordEncoder passwordEncoder,
    EmailService emailService,
    StringRedisTemplate redis,
    @Value("${nexeval.password-reset.token-ttl-minutes:15}") int tokenTtlMinutes,
    @Value("${nexeval.password-reset.rate-limit-seconds:60}") int rateLimitSeconds,
    @Value("${nexeval.password-reset.reset-page-url:https://localhost:5173/reset-password}") String resetPageUrl
  ) {
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
    this.redis = redis;
    this.tokenTtl = Duration.ofMinutes(tokenTtlMinutes);
    this.rateLimitTtl = Duration.ofSeconds(rateLimitSeconds);
    this.resetPageUrl = resetPageUrl;
  }

  public void sendResetCode(String identifier) {
    String normalized = normalizeRequired(identifier, "identifier");

    String rateKey = RATE_PREFIX + normalized;
    if (Boolean.TRUE.equals(redis.hasKey(rateKey))) {
      throw new IllegalArgumentException("操作过于频繁，请 60 秒后再试");
    }

    UserAccount account = userAccountRepository.findByLoginIdentifier(normalized).orElse(null);
    if (account == null) {
      return;
    }

    String email = normalizeNullable(account.getEmail());
    if (email.isBlank()) {
      return;
    }

    redis.opsForValue().setIfAbsent(rateKey, "1", rateLimitTtl);

    String token = UUID.randomUUID().toString().replace("-", "");
    redis.opsForValue().set(TOKEN_PREFIX + token, account.getId(), tokenTtl);

    String resetUrl = resetPageUrl + "?token=" + token;
    emailService.sendResetEmail(email, resetUrl);
  }

  public void resetPassword(String token, String newPassword) {
    String normalizedToken = normalizeRequired(token, "token");
    String password = normalizeRequired(newPassword, "newPassword");

    String redisKey = TOKEN_PREFIX + normalizedToken;
    String userId = redis.opsForValue().get(redisKey);

    if (userId == null) {
      throw new IllegalArgumentException("重置链接已过期或无效");
    }

    if (!isPasswordComplexEnough(password)) {
      throw new IllegalArgumentException("密码需至少满足大写/小写/数字/特殊符号中的三种");
    }

    UserAccount account = userAccountRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    account.setPassword(passwordEncoder.encode(password));
    userAccountRepository.save(account);

    redis.delete(redisKey);

    Set<String> userTokenKeys = redis.keys(TOKEN_PREFIX + "*");
    if (userTokenKeys != null) {
      for (String key : userTokenKeys) {
        String uid = redis.opsForValue().get(key);
        if (userId.equals(uid)) {
          redis.delete(key);
        }
      }
    }
  }

  private boolean isPasswordComplexEnough(String password) {
    int groups = 0;
    if (password.chars().anyMatch(Character::isUpperCase)) groups++;
    if (password.chars().anyMatch(Character::isLowerCase)) groups++;
    if (password.chars().anyMatch(Character::isDigit)) groups++;
    if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) groups++;
    return groups >= 3;
  }

  private String normalizeRequired(String value, String fieldName) {
    String text = value == null ? "" : value.trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return text;
  }

  private String normalizeNullable(String value) {
    return Objects.requireNonNullElse(value, "").trim();
  }
}
