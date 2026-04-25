package com.nexeval.repository;

import com.nexeval.model.TeacherProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, String> {

  Optional<TeacherProfile> findFirstById(String id);
}
