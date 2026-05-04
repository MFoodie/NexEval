package com.nexeval.repository;

import com.nexeval.model.ExamAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
  List<ExamAnswer> findAllBySessionIdOrderByAnsweredAtAsc(String sessionId);

  Optional<ExamAnswer> findFirstBySessionIdAndQuestionId(String sessionId, String questionId);
}
