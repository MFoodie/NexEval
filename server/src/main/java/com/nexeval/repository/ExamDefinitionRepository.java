package com.nexeval.repository;

import com.nexeval.model.ExamDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamDefinitionRepository extends JpaRepository<ExamDefinition, String> {
  Optional<ExamDefinition> findByIdAndActiveTrue(String id);
  Optional<ExamDefinition> findFirstByIsDefaultTrueAndActiveTrue();
}
