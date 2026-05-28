package com.nexeval.repository;

import com.nexeval.model.ExamAttempt;
import com.nexeval.model.SessionMode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
  Optional<ExamAttempt> findFirstBySessionId(String sessionId);

  List<ExamAttempt> findAllByCourseNoAndUserIdAndModeOrderByStartedAtDesc(
    String courseNo,
    String userId,
    SessionMode mode
  );

  List<ExamAttempt> findAllByUserIdAndModeOrderByStartedAtDesc(String userId, SessionMode mode);

  List<ExamAttempt> findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(
    String userId,
    String courseNo,
    SessionMode mode
  );
}
