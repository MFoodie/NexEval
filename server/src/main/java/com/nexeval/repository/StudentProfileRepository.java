package com.nexeval.repository;

import com.nexeval.model.StudentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, String> {

  Optional<StudentProfile> findFirstById(String id);
}
