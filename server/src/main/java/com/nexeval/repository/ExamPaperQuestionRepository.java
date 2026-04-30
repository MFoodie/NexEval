package com.nexeval.repository;

import com.nexeval.model.ExamPaperQuestion;
import com.nexeval.model.ExamPaperQuestionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamPaperQuestionRepository extends JpaRepository<ExamPaperQuestion, ExamPaperQuestionId> {
  List<ExamPaperQuestion> findAllByPaper_IdOrderByDisplayOrderAsc(String paperId);
}
