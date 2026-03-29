package com.youthnightschool.repository;

import com.youthnightschool.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByOpenid(String openid);

    Optional<User> findByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Integer id);

    @Query("SELECT u FROM User u WHERE LOWER(u.nickName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR u.phone LIKE CONCAT('%', :keyword, '%') OR u.openid LIKE CONCAT('%', :keyword, '%')")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
