package com.youthnightschool.repository;

import com.youthnightschool.entity.Session;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    Optional<Session> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}
