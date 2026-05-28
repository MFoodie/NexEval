package com.nexeval.repository;

import com.nexeval.model.PaperPublish;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperPublishRepository extends JpaRepository<PaperPublish, Long> {
  List<PaperPublish> findAllByCnoAndEid(String cno, String eid);
}
