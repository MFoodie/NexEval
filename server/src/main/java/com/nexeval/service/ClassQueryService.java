package com.nexeval.service;

import com.nexeval.dto.ClassStudentSummary;
import com.nexeval.dto.StudentClassSummary;
import com.nexeval.dto.TeacherClassSummary;
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
  private final TeacherProfileRepository teacherProfileRepository;
  private final StudentProfileRepository studentProfileRepository;

  public ClassQueryService(
    TeachingClassRepository teachingClassRepository,
    ScRecordRepository scRecordRepository,
    TeacherProfileRepository teacherProfileRepository,
    StudentProfileRepository studentProfileRepository
  ) {
    this.teachingClassRepository = teachingClassRepository;
    this.scRecordRepository = scRecordRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.studentProfileRepository = studentProfileRepository;
  }

  public List<TeacherClassSummary> getTeacherClasses(String eid) {
    String normalizedEid = required(eid, "eid");
    if (!teacherProfileRepository.existsById(normalizedEid)) {
      throw new IllegalArgumentException("教师工号不存在");
    }

    List<TeachingClassRepository.TeacherClassRow> classes =
      teachingClassRepository.findTeacherClasses(normalizedEid);

    List<TeacherClassSummary> result = new ArrayList<>();
    for (TeachingClassRepository.TeacherClassRow row : classes) {
      List<ClassStudentSummary> students = scRecordRepository
        .findClassStudents(row.getCno(), row.getEid())
        .stream()
        .map(student -> new ClassStudentSummary(
          student.getUserId(),
          student.getSno(),
          student.getName(),
          student.isSex(),
          student.getGrade()
        ))
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

  public List<StudentClassSummary> getStudentClasses(String sno) {
    String normalizedSno = required(sno, "sno");
    if (!studentProfileRepository.existsById(normalizedSno)) {
      throw new IllegalArgumentException("学号不存在");
    }

    return scRecordRepository.findStudentClasses(normalizedSno)
      .stream()
      .map(row -> new StudentClassSummary(
        row.getCno(),
        row.getCname(),
        row.getEid(),
        row.getTeacherName()
      ))
      .toList();
  }

  private String required(String value, String fieldName) {
    String text = value == null ? "" : value.trim();
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " 不能为空");
    }
    return text;
  }
}
