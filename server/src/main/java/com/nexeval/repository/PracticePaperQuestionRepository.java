package com.nexeval.repository;

import com.nexeval.model.PracticePaperQuestion;
import com.nexeval.model.PracticePaperQuestionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticePaperQuestionRepository extends JpaRepository<PracticePaperQuestion, PracticePaperQuestionId> {
  List<PracticePaperQuestion> findAllByPaper_IdOrderByDisplayOrderAsc(String paperId);
}
