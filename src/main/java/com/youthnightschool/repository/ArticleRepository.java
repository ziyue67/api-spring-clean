package com.youthnightschool.repository;

import com.youthnightschool.entity.Article;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Integer> {

    Optional<Article> findByLink(String link);

    Page<Article> findAllByOrderByPublishTimeDescCreateTimeDesc(Pageable pageable);
}
