package com.nexeval.repository;

import com.nexeval.model.EssayQuestionBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EssayQuestionBankRepository extends JpaRepository<EssayQuestionBank, String> {
  List<EssayQuestionBank> findAllByActiveTrueAndCno(String cno);

  boolean existsByActiveTrueAndCno(String cno);
}
