package com.nexeval.service;

import com.nexeval.dto.LoginResponse;
import com.nexeval.dto.StudentInfo;
import com.nexeval.dto.TeacherInfo;
import com.nexeval.model.StudentProfile;
import com.nexeval.model.TeacherProfile;
import com.nexeval.model.UserAccount;
import com.nexeval.repository.StudentProfileRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.repository.UserAccountRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAuthService {

  private static final Pattern BCRYPT_HASH_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

  private final UserAccountRepository userAccountRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final TeacherProfileRepository teacherProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final Path avatarDirectory;

  public UserAuthService(
    UserAccountRepository userAccountRepository,
    StudentProfileRepository studentProfileRepository,
    TeacherProfileRepository teacherProfileRepository,
    PasswordEncoder passwordEncoder,
    @Value("${nexeval.avatar.dir:./avatar}") String avatarDirectory
  ) {
    this.userAccountRepository = userAccountRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.passwordEncoder = passwordEncoder;
    this.avatarDirectory = Path.of(avatarDirectory).toAbsolutePath().normalize();
  }

  public LoginResponse login(String identifier, String rawPassword) {
    String loginIdentifier = normalizeRequired(identifier, "login identifier");
    String password = normalizeRequired(rawPassword, "password");

    UserAccount account = userAccountRepository.findByLoginIdentifier(loginIdentifier)
      .orElseThrow(() -> new IllegalArgumentException("卡号/手机号/邮箱或密码错误"));

    if (!verifyPassword(password, account.getPassword())) {
      throw new IllegalArgumentException("卡号/手机号/邮箱或密码错误");
    }

    if (!isBcryptHash(account.getPassword())) {
      account.setPassword(passwordEncoder.encode(password));
      userAccountRepository.save(account);
    }

    return toLoginResponse(account);
  }

  public LoginResponse updateProfile(
    String userId,
    String name,
    String phone,
    String email,
    String newPassword
  ) {
    String id = normalizeRequired(userId, "userId");
    UserAccount account = userAccountRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    account.setName(normalizeRequired(name, "name"));
    account.setPhone(normalizeRequired(phone, "phone"));
    account.setEmail(normalizeNullable(email));

    String passwordToUpdate = normalizeNullable(newPassword);
    if (!passwordToUpdate.isBlank()) {
      if (!isPasswordComplexEnough(passwordToUpdate)) {
        throw new IllegalArgumentException("密码需至少满足大写/小写/数字/特殊符号中的三种");
      }
      account.setPassword(passwordEncoder.encode(passwordToUpdate));
    }

    userAccountRepository.save(account);
    return toLoginResponse(account);
  }

  public LoginResponse getProfile(String userId) {
    String id = normalizeRequired(userId, "userId");
    UserAccount account = userAccountRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    return toLoginResponse(account);
  }

  public LoginResponse updateAvatar(String userId, String imageBase64) {
    String id = normalizeRequired(userId, "userId");
    String base64 = normalizeRequired(imageBase64, "imageBase64");

    UserAccount account = userAccountRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    byte[] imageBytes = decodeAvatar(base64);

    try {
      Files.createDirectories(avatarDirectory);
      Files.write(
        avatarDirectory.resolve(id + ".png"),
        imageBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      );
    } catch (IOException ex) {
      throw new IllegalStateException("头像保存失败", ex);
    }

    return toLoginResponse(account);
  }

  public LoginResponse resetAvatar(String userId) {
    String id = normalizeRequired(userId, "userId");

    UserAccount account = userAccountRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    try {
      Files.deleteIfExists(avatarDirectory.resolve(id + ".png"));
    } catch (IOException ex) {
      throw new IllegalStateException("恢复默认头像失败", ex);
    }

    return toLoginResponse(account);
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

  private byte[] decodeAvatar(String imageBase64) {
    String lower = imageBase64.toLowerCase();
    if (!lower.startsWith("data:image/png;base64,")
      && !lower.startsWith("data:image/jpeg;base64,")
      && !lower.startsWith("data:image/jpg;base64,")) {
      throw new IllegalArgumentException("仅支持 JPG 或 PNG 图片");
    }

    int comma = imageBase64.indexOf(',');
    if (comma < 0 || comma == imageBase64.length() - 1) {
      throw new IllegalArgumentException("图片数据格式错误");
    }

    try {
      return Base64.getDecoder().decode(imageBase64.substring(comma + 1));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("图片数据格式错误");
    }
  }

  private boolean verifyPassword(String rawPassword, String storedPassword) {
    if (storedPassword == null || storedPassword.isBlank()) {
      return false;
    }

    if (isBcryptHash(storedPassword)) {
      return passwordEncoder.matches(rawPassword, storedPassword);
    }

    return rawPassword.equals(storedPassword);
  }

  private boolean isBcryptHash(String value) {
    return value != null && BCRYPT_HASH_PATTERN.matcher(value).matches();
  }

  private boolean isPasswordComplexEnough(String password) {
    int groups = 0;

    if (password.chars().anyMatch(Character::isUpperCase)) {
      groups++;
    }
    if (password.chars().anyMatch(Character::isLowerCase)) {
      groups++;
    }
    if (password.chars().anyMatch(Character::isDigit)) {
      groups++;
    }
    if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
      groups++;
    }

    return groups >= 3;
  }

  private LoginResponse toLoginResponse(UserAccount account) {
    String type = normalizeType(account.getType());
    String suffix = account.isSex() ? "male" : "female";
    String avatarUrl = resolveAvatarUrl(account.getId(), type, suffix);
    StudentInfo studentInfo = null;
    TeacherInfo teacherInfo = null;

    if ("student".equals(type)) {
      studentInfo = studentProfileRepository.findFirstById(account.getId())
        .map(this::toStudentInfo)
        .orElse(null);
    }

    if ("teacher".equals(type)) {
      teacherInfo = teacherProfileRepository.findFirstById(account.getId())
        .map(this::toTeacherInfo)
        .orElse(null);
    }

    return new LoginResponse(
      account.getId(),
      account.getName(),
      account.isSex(),
      type,
      account.getPhone(),
      account.getEmail(),
      avatarUrl,
      studentInfo,
      teacherInfo
    );
  }

  private StudentInfo toStudentInfo(StudentProfile profile) {
    return new StudentInfo(
      profile.getSno(),
      profile.getEnterYear(),
      profile.getMajor(),
      profile.getDepartment()
    );
  }

  private TeacherInfo toTeacherInfo(TeacherProfile profile) {
    return new TeacherInfo(
      profile.getEid(),
      profile.getEnterYear(),
      toZhTitle(profile.getTitle()),
      "-",
      profile.getDepartment()
    );
  }

  private String toZhTitle(String title) {
    String normalized = normalizeNullable(title).toLowerCase();
    if ("professor".equals(normalized)) {
      return "教授";
    }
    if ("associate_professor".equals(normalized)) {
      return "副教授";
    }
    if ("lecture".equals(normalized) || "lecturer".equals(normalized)) {
      return "讲师";
    }
    return normalized.isBlank() ? "-" : normalized;
  }

  private String normalizeType(String type) {
    String value = normalizeNullable(type).toLowerCase();
    if ("teacher".equals(value) || "admin".equals(value)) {
      return value;
    }
    return "student";
  }

  private String resolveAvatarUrl(String userId, String type, String suffix) {
    Path customAvatar = avatarDirectory.resolve(userId + ".png");
    if (Files.exists(customAvatar)) {
      return "/avatar/" + userId + ".png";
    }

    return "/avatar/" + type + "_" + suffix + ".png";
  }
}
