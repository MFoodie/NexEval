package com.nexeval.repository;

import com.nexeval.model.ScoreAppeal;
import com.nexeval.model.ScoreAppealStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreAppealRepository extends JpaRepository<ScoreAppeal, Long> {
  List<ScoreAppeal> findAllByStatusOrderByCreatedAtDesc(ScoreAppealStatus status);

  List<ScoreAppeal> findAllByUserIdOrderByCreatedAtDesc(String userId);

  List<ScoreAppeal> findAllByOrderByCreatedAtDesc();

  Optional<ScoreAppeal> findFirstBySessionIdAndStatus(String sessionId, ScoreAppealStatus status);
}
