package com.newsportal.repository;

import com.newsportal.model.ArticleView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ArticleViewRepository extends JpaRepository<ArticleView, Long> {
    long countByArticleId(Long articleId);

    @Query("SELECT COUNT(v) FROM ArticleView v WHERE v.timestamp >= :since")
    long countViewsSince(LocalDateTime since);

    @Query("SELECT COUNT(v) FROM ArticleView v WHERE v.article.id = :articleId AND v.timestamp >= :since")
    long countByArticleIdAndTimestampAfter(Long articleId, LocalDateTime since);
}