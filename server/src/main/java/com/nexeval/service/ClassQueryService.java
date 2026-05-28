package com.nexeval.service;

import com.nexeval.dto.ClassStudentSummary;
import com.nexeval.dto.ScoreDistribution;
import com.nexeval.dto.ScoreTierPage;
import com.nexeval.dto.StudentClassSummary;
import com.nexeval.dto.TeacherClassSummary;
import com.nexeval.model.ExamAnswer;
import com.nexeval.model.ExamAttempt;
import com.nexeval.model.ExamAttemptStatus;
import com.nexeval.model.QuestionType;
import com.nexeval.model.SessionMode;
import com.nexeval.repository.BlankQuestionBankRepository;
import com.nexeval.repository.EssayQuestionBankRepository;
import com.nexeval.repository.ExamAnswerRepository;
import com.nexeval.repository.ExamAttemptRepository;
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.QuestionBankRepository;
import com.nexeval.repository.ScRecordRepository;
import com.nexeval.repository.StudentProfileRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.repository.TeachingClassRepository;
import java.util.ArrayList;
import java.util.Comparator;
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
  private final QuestionBankRepository questionBankRepository;
  private final JudgeQuestionBankRepository judgeQuestionBankRepository;
  private final BlankQuestionBankRepository blankQuestionBankRepository;
  private final EssayQuestionBankRepository essayQuestionBankRepository;

  public ClassQueryService(
    TeachingClassRepository teachingClassRepository,
    ScRecordRepository scRecordRepository,
    ExamAttemptRepository examAttemptRepository,
    ExamAnswerRepository examAnswerRepository,
    TeacherProfileRepository teacherProfileRepository,
    StudentProfileRepository studentProfileRepository,
    QuestionBankRepository questionBankRepository,
    JudgeQuestionBankRepository judgeQuestionBankRepository,
    BlankQuestionBankRepository blankQuestionBankRepository,
    EssayQuestionBankRepository essayQuestionBankRepository
  ) {
    this.teachingClassRepository = teachingClassRepository;
    this.scRecordRepository = scRecordRepository;
    this.examAttemptRepository = examAttemptRepository;
    this.examAnswerRepository = examAnswerRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.questionBankRepository = questionBankRepository;
    this.judgeQuestionBankRepository = judgeQuestionBankRepository;
    this.blankQuestionBankRepository = blankQuestionBankRepository;
    this.essayQuestionBankRepository = essayQuestionBankRepository;
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
      List<ScoreRecord> scoreRecords = new ArrayList<>();
      List<ClassStudentSummary> students = scRecordRepository
        .findClassStudents(row.getCno(), row.getEid())
        .stream()
        .map(student -> {
          ScoreRecord sr = resolveStudentScoreRecord(student, row.getCno());
          int grade = student.getGrade() != null ? student.getGrade() : 0;
          int examScore = sr != null ? sr.score() : grade;
          if (sr != null) {
            scoreRecords.add(sr);
          }
          return new ClassStudentSummary(
            student.getUserId(),
            student.getSno(),
            student.getName(),
            student.isSex(),
            examScore
          );
        })
        .toList();

      Double avgScore = null;
      Double passRate = null;
      if (!scoreRecords.isEmpty()) {
        double sum = 0;
        int passCount = 0;
        for (ScoreRecord sr : scoreRecords) {
          sum += sr.score();
          if (sr.percent() >= 60) {
            passCount++;
          }
        }
        avgScore = Math.round(sum / scoreRecords.size() * 10.0) / 10.0;
        passRate = Math.round(passCount * 1000.0 / scoreRecords.size()) / 10.0;
      }

      result.add(new TeacherClassSummary(
        row.getCno(),
        row.getCname(),
        row.getEid(),
        students,
        avgScore,
        passRate
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

  public ScoreDistribution getScoreDistribution(String cno, String eid) {
    String normalizedCno = required(cno, "cno");
    String normalizedEid = required(eid, "eid");

    List<ScRecordRepository.ClassStudentRow> rows =
      scRecordRepository.findClassStudents(normalizedCno, normalizedEid);

    List<ScoreRecord> records = new ArrayList<>();
    for (var row : rows) {
      ScoreRecord sr = resolveStudentScoreRecord(row, normalizedCno);
      if (sr != null) {
        records.add(sr);
      }
    }

    int total = records.size();
    List<ScoreDistribution.TierInfo> tiers = buildTiers(records);

    return new ScoreDistribution(tiers, total);
  }

  public ScoreTierPage getScoreTierStudents(
    String cno, String eid,
    int minPercent, int maxPercent,
    int page, int pageSize,
    String keyword
  ) {
    String normalizedCno = required(cno, "cno");
    String normalizedEid = required(eid, "eid");

    List<ScRecordRepository.ClassStudentRow> rows =
      scRecordRepository.findClassStudents(normalizedCno, normalizedEid);

    List<ScoreRecord> allRecords = new ArrayList<>();
    for (var row : rows) {
      ScoreRecord sr = resolveStudentScoreRecord(row, normalizedCno);
      if (sr != null) {
        allRecords.add(sr);
      }
    }

    List<ScoreRecord> tierRecords = allRecords.stream()
      .filter(r -> r.percent() >= minPercent && r.percent() <= maxPercent)
      .sorted(Comparator.comparingDouble(ScoreRecord::percent).reversed())
      .toList();

    String kw = keyword == null ? "" : keyword.trim().toLowerCase();
    if (!kw.isBlank()) {
      tierRecords = tierRecords.stream()
        .filter(r -> (r.sno() != null && r.sno().toLowerCase().contains(kw))
                  || (r.name() != null && r.name().toLowerCase().contains(kw)))
        .toList();
    }

    long totalCount = tierRecords.size();
    int safePage = Math.max(1, page);
    int safePageSize = Math.max(1, Math.min(pageSize, 100));
    int fromIndex = (safePage - 1) * safePageSize;
    if (fromIndex >= totalCount) {
      return new ScoreTierPage(List.of(), totalCount, safePage, safePageSize);
    }

    int toIndex = Math.min(fromIndex + safePageSize, (int) totalCount);
    List<ScoreTierPage.TierStudent> students = tierRecords.subList(fromIndex, toIndex).stream()
      .map(r -> new ScoreTierPage.TierStudent(
        r.userId(), r.sno(), r.name(), r.sex(),
        r.score(), r.maxScore(), r.percent()
      ))
      .toList();

    return new ScoreTierPage(students, totalCount, safePage, safePageSize);
  }

  private record ScoreRecord(
    String userId, String sno, String name, boolean sex,
    int score, int maxScore, double percent
  ) {}

  private ScoreRecord resolveStudentScoreRecord(
    ScRecordRepository.ClassStudentRow row, String courseNo
  ) {
    String normalizedUserId = normalize(row.getUserId());
    String normalizedCourseNo = normalize(courseNo);
    if (normalizedUserId.isBlank() || normalizedCourseNo.isBlank()) {
      return null;
    }

    List<ExamAttempt> attempts = examAttemptRepository
      .findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(
        normalizedUserId, normalizedCourseNo, SessionMode.EXAM
      );

    for (ExamAttempt attempt : attempts) {
      if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
        continue;
      }

      List<ExamAnswer> answers = examAnswerRepository
        .findAllBySessionIdOrderByAnsweredAtAsc(attempt.getSessionId());

      int totalScore = 0;
      int totalMax = 0;
      for (ExamAnswer answer : answers) {
        Integer s = answer.getScore();
        if (s != null) {
          totalScore += s;
        }
        Integer ms = resolveQuestionMaxScore(answer.getQuestionType(), answer.getQuestionId());
        if (ms != null) {
          totalMax += ms;
        }
      }

      if (totalMax > 0) {
        double percent = Math.round(totalScore * 1000.0 / totalMax) / 10.0;
        return new ScoreRecord(
          row.getUserId(), row.getSno(), row.getName(), row.isSex(),
          totalScore, totalMax, percent
        );
      }
    }

    return null;
  }

  private List<ScoreDistribution.TierInfo> buildTiers(List<ScoreRecord> records) {
    int[] lowerBounds = {0, 60, 70, 80, 90};
    int[] upperBounds = {59, 69, 79, 89, 100};
    String[] labels = {"<60%", "60%-69%", "70%-79%", "80%-89%", "90%-100%"};
    String[] colors = {"#FF0000", "#FF7F27", "#FFC90E", "#C5FA20", "#1C913E"};

    List<ScoreDistribution.TierInfo> tiers = new ArrayList<>();
    for (int i = 0; i < labels.length; i++) {
      int lo = lowerBounds[i];
      int hi = upperBounds[i];
      int count = (int) records.stream()
        .filter(r -> r.percent() >= lo && r.percent() <= hi)
        .count();
      tiers.add(new ScoreDistribution.TierInfo(labels[i], lo, hi, count, colors[i]));
    }
    return tiers;
  }

  private Integer resolveQuestionMaxScore(QuestionType type, String questionId) {
    if (type == null || questionId == null || questionId.isBlank()) {
      return null;
    }

    return switch (type) {
      case CHOICE -> questionBankRepository.findById(questionId)
        .map(com.nexeval.model.QuestionBank::getPoints)
        .orElse(1);
      case JUDGE -> judgeQuestionBankRepository.findById(questionId)
        .map(com.nexeval.model.JudgeQuestionBank::getPoints)
        .orElse(null);
      case BLANK -> blankQuestionBankRepository.findById(questionId)
        .map(com.nexeval.model.BlankQuestionBank::getPoints)
        .orElse(null);
      case ESSAY -> essayQuestionBankRepository.findById(questionId)
        .map(com.nexeval.model.EssayQuestionBank::getPoints)
        .orElse(null);
    };
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
