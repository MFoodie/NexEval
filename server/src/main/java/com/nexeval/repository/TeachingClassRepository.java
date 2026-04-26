package com.nexeval.repository;

import com.nexeval.model.TeachingClass;
import com.nexeval.model.TeachingClassId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeachingClassRepository extends JpaRepository<TeachingClass, TeachingClassId> {
}