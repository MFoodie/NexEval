package com.nexeval.repository;

import com.nexeval.model.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, String> {
}
