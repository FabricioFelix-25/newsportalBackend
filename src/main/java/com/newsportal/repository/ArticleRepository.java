package com.newsportal.repository;

import com.newsportal.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "author")
    Optional<Article> findBySlug(String slug);

    @EntityGraph(attributePaths = "author")
    List<Article> findTop3ByIsDraftFalseAndPublishedAtIsNotNullOrderByPublishedAtDescIdDesc();

    @EntityGraph(attributePaths = "author")
    Page<Article> findByIsDraftFalseOrderByPublishedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByCategoryAndIsDraftFalseOrderByPublishedAtDesc(Article.Category category, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Article> findByAuthorIdAndIsDraftFalseOrderByPublishedAtDesc(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("SELECT a FROM Article a ORDER BY COALESCE(a.updatedAt, a.publishedAt) DESC")
    Page<Article> findAllAdmin(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("SELECT a FROM Article a WHERE a.isDraft = false AND " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.excerpt) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Article> searchByQuery(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t IN :tags AND a.isDraft = false")
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Article a SET a.viewCount = COALESCE(a.viewCount, 0) + 1 WHERE a.id = :id AND a.isDraft = false")
    int incrementPublishedViewCount(@Param("id") Long id);
}
