package com.nexeval.repository;

import com.nexeval.model.ScRecord;
import com.nexeval.model.ScRecordId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScRecordRepository extends JpaRepository<ScRecord, ScRecordId> {
}