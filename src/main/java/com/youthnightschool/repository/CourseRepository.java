package com.youthnightschool.repository;

import com.youthnightschool.entity.Course;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query("select distinct c.month from Course c where c.college = :college order by c.month desc")
    List<Integer> findDistinctMonthsByCollege(String college);

    Page<Course> findByCollegeAndMonth(String college, Integer month, Pageable pageable);

    Page<Course> findByTitleContaining(String title, Pageable pageable);

    Page<Course> findByTitleContainingAndCollege(String title, String college, Pageable pageable);

    Optional<Course> findByLegacyId(String legacyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findAndLockById(Integer id);
}
