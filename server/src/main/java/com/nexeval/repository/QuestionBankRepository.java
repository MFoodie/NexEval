package com.nexeval.repository;

import com.nexeval.model.QuestionBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, String> {
  List<QuestionBank> findAllByActiveTrue();
}
