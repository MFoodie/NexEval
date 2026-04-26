package com.nexeval.service;

import com.nexeval.dto.LoginResponse;
import com.nexeval.dto.StudentInfo;
import com.nexeval.dto.TeacherInfo;
import com.nexeval.dto.AdminRegisterResponse;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthService {

  private static final Pattern BCRYPT_HASH_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");
  private static final String DEFAULT_PASSWORD = "123456";
  private static final String DEFAULT_PHONE = "00000000000";

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

  @Transactional
  public AdminRegisterResponse registerUser(
    String id,
    String name,
    String sexText,
    String typeText,
    String sno,
    String studentEnterYearText,
    String major,
    String studentDepartment,
    String eid,
    String teacherEnterYearText,
    String title,
    String teacherDepartment
  ) {
    String userId = normalizeRequired(id, "id");
    String userName = normalizeRequired(name, "name");
    boolean sex = parseSex(sexText);
    String type = normalizeTypeForRegister(typeText);

    if (userAccountRepository.existsById(userId)) {
      throw new IllegalArgumentException("卡号已存在");
    }

    UserAccount account = new UserAccount();
    account.setId(userId);
    account.setName(userName);
    account.setSex(sex);
    account.setType(type);
    account.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
    account.setPhone(DEFAULT_PHONE);
    account.setEmail(null);
    userAccountRepository.save(account);

    if ("student".equals(type)) {
      registerStudentProfile(userId, sno, studentEnterYearText, major, studentDepartment);
    } else {
      registerTeacherProfile(userId, eid, teacherEnterYearText, title, teacherDepartment);
    }

    return new AdminRegisterResponse(userId, type, "注册成功，默认密码为123456");
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

  private void registerStudentProfile(
    String userId,
    String sno,
    String enterYearText,
    String major,
    String department
  ) {
    String normalizedSno = normalizeRequired(sno, "sno");
    int enterYear = parseYear(enterYearText, "enteryear");
    String normalizedMajor = normalizeRequired(major, "major");
    String normalizedDepartment = normalizeRequired(department, "department");

    if (studentProfileRepository.existsById(normalizedSno)) {
      throw new IllegalArgumentException("学号已存在");
    }

    StudentProfile profile = new StudentProfile();
    profile.setId(userId);
    profile.setSno(normalizedSno);
    profile.setEnterYear(enterYear);
    profile.setMajor(normalizedMajor);
    profile.setDepartment(normalizedDepartment);
    studentProfileRepository.save(profile);
  }

  private void registerTeacherProfile(
    String userId,
    String eid,
    String enterYearText,
    String title,
    String department
  ) {
    String normalizedEid = normalizeRequired(eid, "eid");
    int enterYear = parseYear(enterYearText, "enteryear");
    String normalizedTitle = normalizeTitle(title);
    String normalizedDepartment = normalizeRequired(department, "department");

    if (teacherProfileRepository.existsById(normalizedEid)) {
      throw new IllegalArgumentException("工号已存在");
    }

    TeacherProfile profile = new TeacherProfile();
    profile.setId(userId);
    profile.setEid(normalizedEid);
    profile.setEnterYear(enterYear);
    profile.setTitle(normalizedTitle);
    profile.setDepartment(normalizedDepartment);
    teacherProfileRepository.save(profile);
  }

  private String normalizeTypeForRegister(String value) {
    String type = normalizeRequired(value, "type").toLowerCase();
    if (!"teacher".equals(type) && !"student".equals(type)) {
      throw new IllegalArgumentException("type 仅支持 teacher 或 student");
    }
    return type;
  }

  private boolean parseSex(String value) {
    String text = normalizeRequired(value, "sex");
    if ("男".equals(text)) {
      return true;
    }
    if ("女".equals(text)) {
      return false;
    }
    throw new IllegalArgumentException("sex 仅支持 男 或 女");
  }

  private int parseYear(String value, String fieldName) {
    String text = normalizeRequired(value, fieldName);
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(fieldName + " 必须是数字");
    }
  }

  private String normalizeTitle(String value) {
    String text = normalizeRequired(value, "title").toLowerCase();
    if ("professor".equals(text) || "associate_professor".equals(text) || "lecture".equals(text)) {
      return text;
    }
    throw new IllegalArgumentException("title 仅支持 professor、associate_professor、lecture");
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
