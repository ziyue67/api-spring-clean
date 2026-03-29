package com.youthnightschool.repository;

import com.youthnightschool.entity.CourseSignup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSignupRepository extends JpaRepository<CourseSignup, Integer> {

    Optional<CourseSignup> findByCourseIdAndUserId(Integer courseId, Integer userId);

    long countByCourseIdAndStatus(Integer courseId, String status);

    long countByStatus(String status);

    long countByCourseId(Integer courseId);

    Optional<CourseSignup> findFirstByCourseIdAndStatusOrderByCreatedAtAscIdAsc(Integer courseId, String status);

    List<CourseSignup> findByCourseIdInAndStatus(List<Integer> courseIds, String status);

    List<CourseSignup> findByCourseId(Integer courseId);

    List<CourseSignup> findByUserIdOrderByCreatedAtDesc(Integer userId);

    void deleteByCourseIdAndUserId(Integer courseId, Integer userId);
}
