package com.nexeval.repository;

import com.nexeval.model.PracticePaper;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticePaperRepository extends JpaRepository<PracticePaper, String> {
  Optional<PracticePaper> findFirstByActiveTrueAndCourseNo(String courseNo);

  Optional<PracticePaper> findFirstByActiveTrueAndCourseNoIsNull();
}
