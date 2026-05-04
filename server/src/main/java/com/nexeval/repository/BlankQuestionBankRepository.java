package com.nexeval.repository;

import com.nexeval.model.BlankQuestionBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlankQuestionBankRepository extends JpaRepository<BlankQuestionBank, String> {
  List<BlankQuestionBank> findAllByActiveTrueAndCno(String cno);

  boolean existsByActiveTrueAndCno(String cno);
}
