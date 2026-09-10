package com.hirehub.dao;

import com.hirehub.domain.JobApplication;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ApplicationDao extends JpaRepository<JobApplication, Long> {
  boolean existsByUserIdAndJobId(Long userId, Long jobId);

  @EntityGraph(attributePaths = {"user", "job"})
  Page<JobApplication> findByUserEmail(String email, Pageable page);

  @Override
  @EntityGraph(attributePaths = {"user", "job"})
  Page<JobApplication> findAll(Pageable page);
}
