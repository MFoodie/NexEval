package com.nexeval.service;

import com.nexeval.dto.BulkImportResult;
import com.nexeval.model.Course;
import com.nexeval.model.ScRecord;
import com.nexeval.model.ScRecordId;
import com.nexeval.model.StudentProfile;
import com.nexeval.model.TeacherProfile;
import com.nexeval.model.TeachingClass;
import com.nexeval.model.TeachingClassId;
import com.nexeval.model.UserAccount;
import com.nexeval.repository.CourseRepository;
import com.nexeval.repository.ScRecordRepository;
import com.nexeval.repository.StudentProfileRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.repository.TeachingClassRepository;
import com.nexeval.repository.UserAccountRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {

  private static final String DEFAULT_PASSWORD = "123456";
  private static final String DEFAULT_PHONE = "00000000000";

  private final UserAccountRepository userAccountRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final TeacherProfileRepository teacherProfileRepository;
  private final CourseRepository courseRepository;
  private final TeachingClassRepository teachingClassRepository;
  private final ScRecordRepository scRecordRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminManagementService(
    UserAccountRepository userAccountRepository,
    StudentProfileRepository studentProfileRepository,
    TeacherProfileRepository teacherProfileRepository,
    CourseRepository courseRepository,
    TeachingClassRepository teachingClassRepository,
    ScRecordRepository scRecordRepository,
    PasswordEncoder passwordEncoder
  ) {
    this.userAccountRepository = userAccountRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.courseRepository = courseRepository;
    this.teachingClassRepository = teachingClassRepository;
    this.scRecordRepository = scRecordRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public Map<String, Object> createCourse(String cno, String cname, String creditText) {
    String normalizedCno = required(cno, "cno");
    String normalizedCname = required(cname, "cname");
    BigDecimal credit = parseCredit(creditText);

    if (courseRepository.existsById(normalizedCno)) {
      throw new IllegalArgumentException("课程编号已存在");
    }

    Course course = new Course();
    course.setCno(normalizedCno);
    course.setCname(normalizedCname);
    course.setCredit(credit);
    courseRepository.save(course);

    return Map.of("message", "课程创建成功", "cno", normalizedCno);
  }

  @Transactional
  public Map<String, Object> createTeachingClass(String cno, String eid) {
    String normalizedCno = required(cno, "cno");
    String normalizedEid = required(eid, "eid");

    if (!courseRepository.existsById(normalizedCno)) {
      throw new IllegalArgumentException("课程编号不存在");
    }
    if (!teacherProfileRepository.existsById(normalizedEid)) {
      throw new IllegalArgumentException("教师工号不存在");
    }

    TeachingClassId id = new TeachingClassId();
    id.setCno(normalizedCno);
    id.setEid(normalizedEid);

    if (teachingClassRepository.existsById(id)) {
      throw new IllegalArgumentException("教学班已存在");
    }

    TeachingClass clazz = new TeachingClass();
    clazz.setId(id);
    teachingClassRepository.save(clazz);

    return Map.of("message", "教学班创建成功", "cno", normalizedCno, "eid", normalizedEid);
  }

  public BulkImportResult importBatch(String importType, String fileBase64) {
    String type = required(importType, "importType").toUpperCase(Locale.ROOT);
    byte[] fileBytes = decodeBase64File(fileBase64);

    DataFormatter formatter = new DataFormatter();
    List<String> errors = new ArrayList<>();
    int imported = 0;
    int skipped = 0;

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
      Sheet sheet = workbook.getSheetAt(0);

      for (int i = 0; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        try {
          RowResult result = switch (type) {
            case "STUDENTS" -> importStudentRow(row, formatter);
            case "TEACHERS" -> importTeacherRow(row, formatter);
            case "COURSES" -> importCourseRow(row, formatter);
            case "CLASSES" -> importClassRow(row, formatter);
            case "SC" -> importScRow(row, formatter);
            default -> throw new IllegalArgumentException("不支持的导入类型: " + type);
          };

          if (result == RowResult.IMPORTED) {
            imported++;
          } else if (result == RowResult.SKIPPED) {
            skipped++;
          }
        } catch (RuntimeException ex) {
          if (errors.size() < 20) {
            errors.add("第 " + (i + 1) + " 行: " + ex.getMessage());
          }
        }
      }
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalArgumentException("文件解析失败，请确认上传的是 xlsx 文件");
    }

    return new BulkImportResult(type, imported, skipped, errors.size(), errors);
  }

  private RowResult importStudentRow(Row row, DataFormatter formatter) {
    List<String> values = getRowValues(row, formatter, 7);
    if (isBlankRow(values)) {
      return RowResult.SKIPPED;
    }

    String id = required(values.get(0), "卡号");
    String name = required(values.get(1), "姓名");
    boolean sex = parseSex(values.get(2));
    String sno = required(values.get(3), "学号");
    int enterYear = parseYear(values.get(4), "入学年份");
    String major = required(values.get(5), "专业");
    String department = required(values.get(6), "学院");

    if (userAccountRepository.existsById(id) || studentProfileRepository.existsById(sno)) {
      return RowResult.SKIPPED;
    }

    UserAccount account = new UserAccount();
    account.setId(id);
    account.setName(name);
    account.setSex(sex);
    account.setType("student");
    account.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
    account.setPhone(DEFAULT_PHONE);
    account.setEmail(null);
    userAccountRepository.save(account);

    StudentProfile profile = new StudentProfile();
    profile.setId(id);
    profile.setSno(sno);
    profile.setEnterYear(enterYear);
    profile.setMajor(major);
    profile.setDepartment(department);
    studentProfileRepository.save(profile);

    return RowResult.IMPORTED;
  }

  private RowResult importTeacherRow(Row row, DataFormatter formatter) {
    List<String> values = getRowValues(row, formatter, 7);
    if (isBlankRow(values)) {
      return RowResult.SKIPPED;
    }

    String id = required(values.get(0), "卡号");
    String name = required(values.get(1), "姓名");
    boolean sex = parseSex(values.get(2));
    String eid = required(values.get(3), "工号");
    int enterYear = parseYear(values.get(4), "入职年份");
    String title = normalizeTitle(values.get(5));
    String department = required(values.get(6), "学院");

    if (userAccountRepository.existsById(id) || teacherProfileRepository.existsById(eid)) {
      return RowResult.SKIPPED;
    }

    UserAccount account = new UserAccount();
    account.setId(id);
    account.setName(name);
    account.setSex(sex);
    account.setType("teacher");
    account.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
    account.setPhone(DEFAULT_PHONE);
    account.setEmail(null);
    userAccountRepository.save(account);

    TeacherProfile profile = new TeacherProfile();
    profile.setId(id);
    profile.setEid(eid);
    profile.setEnterYear(enterYear);
    profile.setTitle(title);
    profile.setDepartment(department);
    teacherProfileRepository.save(profile);

    return RowResult.IMPORTED;
  }

  private RowResult importCourseRow(Row row, DataFormatter formatter) {
    List<String> values = getRowValues(row, formatter, 3);
    if (isBlankRow(values)) {
      return RowResult.SKIPPED;
    }

    String cno = required(values.get(0), "课程编号");
    String cname = required(values.get(1), "课程名称");
    BigDecimal credit = parseCredit(values.get(2));

    if (courseRepository.existsById(cno)) {
      return RowResult.SKIPPED;
    }

    Course course = new Course();
    course.setCno(cno);
    course.setCname(cname);
    course.setCredit(credit);
    courseRepository.save(course);

    return RowResult.IMPORTED;
  }

  private RowResult importClassRow(Row row, DataFormatter formatter) {
    List<String> values = getRowValues(row, formatter, 2);
    if (isBlankRow(values)) {
      return RowResult.SKIPPED;
    }

    String cno = required(values.get(0), "课程编号");
    String eid = required(values.get(1), "教师工号");

    if (!courseRepository.existsById(cno) || !teacherProfileRepository.existsById(eid)) {
      throw new IllegalArgumentException("课程编号或教师工号不存在");
    }

    TeachingClassId id = new TeachingClassId();
    id.setCno(cno);
    id.setEid(eid);

    if (teachingClassRepository.existsById(id)) {
      return RowResult.SKIPPED;
    }

    TeachingClass clazz = new TeachingClass();
    clazz.setId(id);
    teachingClassRepository.save(clazz);

    return RowResult.IMPORTED;
  }

  private RowResult importScRow(Row row, DataFormatter formatter) {
    List<String> values = getRowValues(row, formatter, 4);
    if (isBlankRow(values)) {
      return RowResult.SKIPPED;
    }

    String sno = required(values.get(0), "学号");
    String cno = required(values.get(1), "课程编号");
    String eid = required(values.get(2), "教师工号");
    Integer grade = parseOptionalGrade(values.get(3));

    if (!studentProfileRepository.existsById(sno)) {
      throw new IllegalArgumentException("学号不存在");
    }

    TeachingClassId classId = new TeachingClassId();
    classId.setCno(cno);
    classId.setEid(eid);
    if (!teachingClassRepository.existsById(classId)) {
      throw new IllegalArgumentException("教学班不存在");
    }

    ScRecordId id = new ScRecordId();
    id.setSno(sno);
    id.setCno(cno);
    id.setEid(eid);

    if (scRecordRepository.existsById(id)) {
      return RowResult.SKIPPED;
    }

    ScRecord record = new ScRecord();
    record.setId(id);
    record.setGrade(grade);
    scRecordRepository.save(record);

    return RowResult.IMPORTED;
  }

  private List<String> getRowValues(Row row, DataFormatter formatter, int cellCount) {
    List<String> values = new ArrayList<>(cellCount);
    for (int i = 0; i < cellCount; i++) {
      values.add(formatter.formatCellValue(row.getCell(i)).trim());
    }
    return values;
  }

  private boolean isBlankRow(List<String> values) {
    return values.stream().allMatch(String::isBlank);
  }

  private byte[] decodeBase64File(String fileBase64) {
    String raw = required(fileBase64, "fileBase64");
    int commaIndex = raw.indexOf(',');
    String value = commaIndex >= 0 ? raw.substring(commaIndex + 1) : raw;
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("文件内容不是有效的 Base64");
    }
  }

  private String required(String value, String fieldName) {
    String text = value == null ? "" : value.trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " 不能为空");
    }
    return text;
  }

  private boolean parseSex(String value) {
    String text = required(value, "性别");
    if ("男".equals(text)) {
      return true;
    }
    if ("女".equals(text)) {
      return false;
    }
    throw new IllegalArgumentException("性别仅支持 男 或 女");
  }

  private int parseYear(String value, String fieldName) {
    String text = required(value, fieldName);
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(fieldName + " 必须是数字");
    }
  }

  private BigDecimal parseCredit(String value) {
    String text = required(value, "学分");
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("学分格式不正确");
    }
  }

  private Integer parseOptionalGrade(String value) {
    if (value == null || value.trim().isBlank()) {
      return null;
    }

    try {
      int grade = Integer.parseInt(value.trim());
      if (grade < 0 || grade > 100) {
        throw new IllegalArgumentException("成绩必须在 0-100 之间");
      }
      return grade;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("成绩必须是数字");
    }
  }

  private String normalizeTitle(String value) {
    String text = required(value, "职称");

    Map<String, String> titleMap = new LinkedHashMap<>();
    titleMap.put("教授", "professor");
    titleMap.put("副教授", "associate_professor");
    titleMap.put("讲师", "lecture");
    titleMap.put("professor", "professor");
    titleMap.put("associate_professor", "associate_professor");
    titleMap.put("lecture", "lecture");

    String normalized = titleMap.get(text.toLowerCase(Locale.ROOT));
    if (normalized == null) {
      normalized = titleMap.get(text);
    }
    if (normalized == null) {
      throw new IllegalArgumentException("职称仅支持 教授/副教授/讲师");
    }
    return normalized;
  }

  private enum RowResult {
    IMPORTED,
    SKIPPED
  }
}