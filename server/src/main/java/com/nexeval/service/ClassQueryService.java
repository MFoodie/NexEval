package com.nexeval.service;

import com.nexeval.dto.ClassStudentSummary;
import com.nexeval.dto.StudentClassSummary;
import com.nexeval.dto.TeacherClassSummary;
import com.nexeval.model.ExamAttempt;
import com.nexeval.model.ExamAttemptStatus;
import com.nexeval.model.SessionMode;
import com.nexeval.repository.ExamAnswerRepository;
import com.nexeval.repository.ExamAttemptRepository;
import com.nexeval.repository.ScRecordRepository;
import com.nexeval.repository.StudentProfileRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.repository.TeachingClassRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClassQueryService {

  private final TeachingClassRepository teachingClassRepository;
  private final ScRecordRepository scRecordRepository;
  private final ExamAttemptRepository examAttemptRepository;
  private final ExamAnswerRepository examAnswerRepository;
  private final TeacherProfileRepository teacherProfileRepository;
  private final StudentProfileRepository studentProfileRepository;

  public ClassQueryService(
    TeachingClassRepository teachingClassRepository,
    ScRecordRepository scRecordRepository,
    ExamAttemptRepository examAttemptRepository,
    ExamAnswerRepository examAnswerRepository,
    TeacherProfileRepository teacherProfileRepository,
    StudentProfileRepository studentProfileRepository
  ) {
    this.teachingClassRepository = teachingClassRepository;
    this.scRecordRepository = scRecordRepository;
    this.examAttemptRepository = examAttemptRepository;
    this.examAnswerRepository = examAnswerRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.studentProfileRepository = studentProfileRepository;
  }

  public List<TeacherClassSummary> getTeacherClasses(String eid) {
    String normalizedEid = required(eid, "eid");
    if (teacherProfileRepository.findFirstByEid(normalizedEid).isEmpty()) {
      throw new IllegalArgumentException("教师工号不存在");
    }

    List<TeachingClassRepository.TeacherClassRow> classes =
      teachingClassRepository.findTeacherClasses(normalizedEid);

    List<TeacherClassSummary> result = new ArrayList<>();
    for (TeachingClassRepository.TeacherClassRow row : classes) {
      List<ClassStudentSummary> students = scRecordRepository
        .findClassStudents(row.getCno(), row.getEid())
        .stream()
        .map(student -> {
          Integer examScore = resolveLatestExamScore(student.getUserId(), row.getCno());
          return new ClassStudentSummary(
            student.getUserId(),
            student.getSno(),
            student.getName(),
            student.isSex(),
            examScore != null ? examScore : student.getGrade()
          );
        })
        .toList();

      result.add(new TeacherClassSummary(
        row.getCno(),
        row.getCname(),
        row.getEid(),
        students
      ));
    }

    return result;
  }

  public List<StudentClassSummary> getStudentClasses(String sno, String userId) {
    String normalizedSno = sno == null ? "" : sno.trim();
    String normalizedUserId = userId == null ? "" : userId.trim();
    if (normalizedSno.isBlank()) {
      normalizedSno = resolveStudentSnoByUserId(normalizedUserId);
    }
    return getStudentClasses(normalizedSno);
  }

  private String resolveStudentSnoByUserId(String userId) {
    String normalizedUserId = required(userId, "userId");
    var studentProfile = studentProfileRepository.findFirstById(normalizedUserId)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    String sno = studentProfile.getSno();
    if (sno == null || sno.isBlank()) {
      throw new IllegalArgumentException("学生信息不足");
    }
    return sno;
  }

  public List<StudentClassSummary> getStudentClasses(String sno) {
    String normalizedSno = required(sno, "sno");
    var studentProfile = studentProfileRepository.findFirstBySno(normalizedSno)
      .orElseThrow(() -> new IllegalArgumentException("学号不存在"));

    String userId = studentProfile.getId();
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("学号不存在");
    }

    return scRecordRepository.findStudentClasses(normalizedSno)
      .stream()
      .map(row -> {
        Integer examScore = resolveLatestExamScore(userId, row.getCno());
        return new StudentClassSummary(
          row.getCno(),
          row.getCname(),
          row.getEid(),
          row.getTeacherName(),
          examScore != null ? examScore : row.getGrade()
        );
      })
      .toList();
  }

  private String required(String value, String fieldName) {
    String text = value == null ? "" : value.trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " 不能为空");
    }
    return text;
  }

  private Integer resolveLatestExamScore(String userId, String courseNo) {
    String normalizedUserId = normalize(userId);
    String normalizedCourseNo = normalize(courseNo);
    if (normalizedUserId.isBlank() || normalizedCourseNo.isBlank()) {
      return null;
    }

    List<ExamAttempt> attempts = examAttemptRepository
      .findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(
        normalizedUserId,
        normalizedCourseNo,
        SessionMode.EXAM
      );

    for (ExamAttempt attempt : attempts) {
      if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
        continue;
      }
      Long total = examAnswerRepository.sumScoreBySessionId(attempt.getSessionId());
      return total == null ? 0 : total.intValue();
    }

    return null;
  }

  public java.util.List<com.nexeval.dto.CourseScoreSummary> searchCourseScoresForStudent(
    String userId,
    String keyword
  ) {
    String normalizedUserId = normalize(userId);
    if (normalizedUserId.isBlank()) {
      throw new IllegalArgumentException("userId 不能为空");
    }

    var studentProfile = studentProfileRepository.findFirstById(normalizedUserId)
      .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    String sno = studentProfile.getSno();
    if (sno == null || sno.isBlank()) {
      throw new IllegalArgumentException("学生信息不足");
    }

    String kw = keyword == null ? "" : keyword.trim().toLowerCase();

    var classes = scRecordRepository.findStudentClasses(sno)
      .stream()
      .filter(row -> {
        if (kw.isBlank()) return true;
        if (row.getCno() != null && row.getCno().toLowerCase().equals(kw)) return true;
        if (row.getCname() != null && row.getCname().toLowerCase().contains(kw)) return true;
        return false;
      })
      .toList();

    java.util.List<com.nexeval.dto.CourseScoreSummary> result = new java.util.ArrayList<>();

    for (var row : classes) {
      String cno = row.getCno();
      String cname = row.getCname();

      // compute student's own score
      Integer studentScore = resolveLatestExamScore(normalizedUserId, cno);
      if (studentScore == null) {
        studentScore = row.getGrade();
      }

      // compute class stats
      var students = scRecordRepository.findClassStudents(cno, row.getEid());
      java.util.List<Integer> scores = new java.util.ArrayList<>();
      for (var s : students) {
        Integer sc = resolveLatestExamScore(s.getUserId(), cno);
        if (sc == null) {
          sc = s.getGrade();
        }
        if (sc != null) scores.add(sc);
      }

      Integer max = null;
      Integer min = null;
      Double avg = null;
      if (!scores.isEmpty()) {
        int sum = 0;
        int m = Integer.MIN_VALUE;
        int n = Integer.MAX_VALUE;
        for (int v : scores) {
          sum += v;
          if (v > m) m = v;
          if (v < n) n = v;
        }
        max = m;
        min = n;
        avg = (double) sum / scores.size();
      }

      result.add(new com.nexeval.dto.CourseScoreSummary(
        cno,
        cname,
        row.getEid(),
        row.getTeacherName(),
        studentScore,
        max,
        min,
        avg
      ));
    }

    return result;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
