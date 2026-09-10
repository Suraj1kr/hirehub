package com.hirehub.dao;

import com.hirehub.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDao extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByEmail(String email);

  boolean existsByEmail(String email);
}
