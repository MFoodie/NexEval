package com.nexeval.repository;

import com.nexeval.model.JudgeQuestionBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JudgeQuestionBankRepository extends JpaRepository<JudgeQuestionBank, String> {
  List<JudgeQuestionBank> findAllByActiveTrueAndCno(String cno);

  boolean existsByActiveTrueAndCno(String cno);
}
