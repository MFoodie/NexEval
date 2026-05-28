package com.nexeval.repository;

import com.nexeval.model.PaperQuestionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperQuestionItemRepository extends JpaRepository<PaperQuestionItem, Long> {
  List<PaperQuestionItem> findAllByPaperIdOrderByDisplayOrderAsc(String paperId);
}
