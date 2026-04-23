package com.nexeval.repository;

import com.nexeval.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

  @Query("""
    select u
    from UserAccount u
    where u.id = :identifier
       or u.phone = :identifier
       or lower(coalesce(u.email, '')) = lower(:identifier)
    """)
  Optional<UserAccount> findByLoginIdentifier(@Param("identifier") String identifier);
}
