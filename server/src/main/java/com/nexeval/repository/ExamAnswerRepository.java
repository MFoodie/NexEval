package com.nexeval.repository;

import com.nexeval.model.ExamAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
  List<ExamAnswer> findAllBySessionIdOrderByAnsweredAtAsc(String sessionId);

  Optional<ExamAnswer> findFirstBySessionIdAndQuestionId(String sessionId, String questionId);

  @Query("select coalesce(sum(a.score), 0) from ExamAnswer a where a.sessionId = :sessionId")
  Long sumScoreBySessionId(@Param("sessionId") String sessionId);
}
