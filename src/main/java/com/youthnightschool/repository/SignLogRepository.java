package com.youthnightschool.repository;

import com.youthnightschool.entity.SignLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignLogRepository extends JpaRepository<SignLog, Integer> {

    boolean existsByUserIdAndSignDate(Integer userId, java.time.LocalDate signDate);

    long countByUserId(Integer userId);

    List<SignLog> findTop6ByUserIdOrderByCreatedAtDesc(Integer userId);

    List<SignLog> findByCreatedAtAfter(Instant since);
}
