package com.newsportal.repository;

import com.newsportal.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);

    List<Article> findByFeaturedTrueAndIsDraftFalseOrderByPublishedAtDesc();

    Page<Article> findByIsDraftFalseOrderByPublishedAtDesc(Pageable pageable);

    Page<Article> findByCategoryAndIsDraftFalseOrderByPublishedAtDesc(Article.Category category, Pageable pageable);

    Page<Article> findByAuthorIdAndIsDraftFalseOrderByPublishedAtDesc(Long authorId, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.isDraft = false AND " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.excerpt) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Article> searchByQuery(@Param("query") String query, Pageable pageable);

    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t IN :tags AND a.isDraft = false")
    Page<Article> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    @Query("SELECT DISTINCT a.category FROM Article a WHERE a.isDraft = false")
    List<Article.Category> findDistinctCategories();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.isDraft = false")
    long countPublishedArticles();

    @Query("SELECT COUNT(a) FROM Article a WHERE a.isDraft = true")
    long countDraftArticles();

    boolean existsBySlug(String slug);

    @Query("SELECT COUNT(a) > 0 FROM Article a WHERE a.slug = :slug AND a.id != :id")
    boolean existsBySlugAndIdNot(String slug, Long id);
}