package com.newsportal.service;

import com.newsportal.dto.ArticleRequest;
import com.newsportal.dto.ArticleResponse;
import com.newsportal.exception.BadRequestException;
import com.newsportal.exception.ResourceNotFoundException;
import com.newsportal.model.Article;
import com.newsportal.model.ArticleView;
import com.newsportal.model.Author;
import com.newsportal.repository.ArticleRepository;
import com.newsportal.repository.ArticleViewRepository;
import com.newsportal.repository.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ArticleViewRepository articleViewRepository;

    public Page<ArticleResponse> getAllPublishedArticles(Pageable pageable) {
        return articleRepository.findByIsDraftFalseOrderByPublishedAtDesc(pageable)
                .map(ArticleResponse::new);
    }

    public Page<ArticleResponse> getAllArticlesForAdmin(Pageable pageable) {
        return articleRepository.findAllAdmin(pageable)
                .map(ArticleResponse::new);
    }

    public List<ArticleResponse> getFeaturedArticles() {
        return articleRepository.findByFeaturedTrueAndIsDraftFalseOrderByPublishedAtDesc()
                .stream()
                .map(ArticleResponse::new)
                .collect(Collectors.toList());
    }

    public Page<ArticleResponse> getArticlesByCategory(String category, Pageable pageable) {
        Article.Category categoryEnum = parseCategory(category);
        return articleRepository.findByCategoryAndIsDraftFalseOrderByPublishedAtDesc(categoryEnum, pageable)
                .map(ArticleResponse::new);
    }

    public Page<ArticleResponse> getArticlesByAuthor(Long authorId, Pageable pageable) {
        return articleRepository.findByAuthorIdAndIsDraftFalseOrderByPublishedAtDesc(authorId, pageable)
                .map(ArticleResponse::new);
    }

    public Page<ArticleResponse> searchArticles(String query, String tag, Pageable pageable) {
        if (tag != null && !tag.isEmpty()) {
            return articleRepository.findByTagsIn(Arrays.asList(tag), pageable)
                    .map(ArticleResponse::new);
        } else if (query != null && !query.isEmpty()) {
            return articleRepository.searchByQuery(query, pageable)
                    .map(ArticleResponse::new);
        } else {
            return getAllPublishedArticles(pageable);
        }
    }

    public ArticleResponse getArticleById(Long id, boolean includeDrafts) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        if (!includeDrafts && Boolean.TRUE.equals(article.getIsDraft())) {
            throw new ResourceNotFoundException("Article not found with id: " + id);
        }
        return new ArticleResponse(article);
    }

    public ArticleResponse getArticleBySlug(String slug, boolean includeDrafts) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
        if (!includeDrafts && Boolean.TRUE.equals(article.getIsDraft())) {
            throw new ResourceNotFoundException("Article not found with slug: " + slug);
        }
        return new ArticleResponse(article);
    }

    public ArticleResponse createArticle(ArticleRequest request) {
        validateEditorialChecklist(request);

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + request.getAuthorId()));

        String slug = generateSlug(request.getTitle());
        if (articleRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Article article = new Article();
        article.setSlug(slug);
        article.setTitle(request.getTitle());
        article.setSubtitle(request.getSubtitle());
        article.setContent(request.getContent());
        article.setExcerpt(request.getExcerpt());
        article.setImageUrl(request.getImageUrl());
        article.setCategory(parseCategory(request.getCategory()));
        article.setTags(request.getTags());
        article.setAuthor(author);
        article.setFeatured(request.getFeatured());
        article.setIsDraft(request.getIsDraft());
        article.setSeoTitle(request.getSeoTitle());
        article.setSeoDescription(request.getSeoDescription());
        article.setSeoImage(request.getSeoImage());
        applyEditorialFields(article, request);

        Article savedArticle = articleRepository.save(article);
        return new ArticleResponse(savedArticle);
    }

    public ArticleResponse updateArticle(Long id, ArticleRequest request) {
        validateEditorialChecklist(request);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + request.getAuthorId()));

        article.setTitle(request.getTitle());
        article.setSubtitle(request.getSubtitle());
        article.setContent(request.getContent());
        article.setExcerpt(request.getExcerpt());
        article.setImageUrl(request.getImageUrl());
        article.setCategory(parseCategory(request.getCategory()));
        article.setTags(request.getTags());
        article.setAuthor(author);
        article.setFeatured(request.getFeatured());
        article.setIsDraft(request.getIsDraft());
        article.setSeoTitle(request.getSeoTitle());
        article.setSeoDescription(request.getSeoDescription());
        article.setSeoImage(request.getSeoImage());
        applyEditorialFields(article, request);

        Article savedArticle = articleRepository.save(article);
        return new ArticleResponse(savedArticle);
    }

    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article not found with id: " + id);
        }
        articleRepository.deleteById(id);
    }

    public void trackView(Long articleId, String userAgent, String ipAddress) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        ArticleView view = new ArticleView();
        view.setArticle(article);
        view.setUserAgent(userAgent);
        view.setIpAddress(ipAddress);

        articleViewRepository.save(view);

        // Update view count
        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);
    }

    public List<String> getCategories() {
        return articleRepository.findDistinctCategories()
                .stream()
                .map(category -> category.name().toLowerCase().replace('_', '-'))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalArticles", articleRepository.count());
        stats.put("publishedArticles", articleRepository.countPublishedArticles());
        stats.put("draftArticles", articleRepository.countDraftArticles());
        stats.put("categories", articleRepository.findDistinctCategories().size());
        stats.put("recentViews", articleViewRepository.countViewsSince(LocalDateTime.now().minusDays(7)));
        return stats;
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private Article.Category parseCategory(String category) {
        try {
            return Article.Category.valueOf(category.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + category);
        }
    }

    private void applyEditorialFields(Article article, ArticleRequest request) {
        article.setAiAssisted(Boolean.TRUE.equals(request.getAiAssisted()));
        article.setSourceReferences(request.getSourceReferences());
        article.setReviewedBy(request.getReviewedBy());
        article.setFactChecked(Boolean.TRUE.equals(request.getFactChecked()));
        article.setRightsCleared(Boolean.TRUE.equals(request.getRightsCleared()));
        article.setSensitiveContentReviewed(Boolean.TRUE.equals(request.getSensitiveContentReviewed()));
    }

    private void validateEditorialChecklist(ArticleRequest request) {
        if (Boolean.TRUE.equals(request.getIsDraft())) {
            return;
        }

        if (isBlank(request.getSourceReferences())) {
            throw new BadRequestException("Source references are required before publishing");
        }
        if (isBlank(request.getReviewedBy())) {
            throw new BadRequestException("Reviewer name is required before publishing");
        }
        if (!Boolean.TRUE.equals(request.getFactChecked())) {
            throw new BadRequestException("Fact check must be confirmed before publishing");
        }
        if (!Boolean.TRUE.equals(request.getRightsCleared())) {
            throw new BadRequestException("Image/source rights must be confirmed before publishing");
        }
        if (!Boolean.TRUE.equals(request.getSensitiveContentReviewed())) {
            throw new BadRequestException("Sensitive content review must be confirmed before publishing");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
