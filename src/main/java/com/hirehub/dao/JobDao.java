package com.hirehub.dao;
import com.hirehub.domain.Job;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface JobDao extends JpaRepository<Job,Long> {
 @Query("select j from Job j where j.active=true and (lower(j.title) like lower(concat('%',:q,'%')) or lower(j.company) like lower(concat('%',:q,'%')) or lower(j.location) like lower(concat('%',:q,'%')))")
 Page<Job> search(@Param("q") String q,Pageable page);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select j from Job j where j.id=:id") Optional<Job> lockById(@Param("id") Long id);
}
