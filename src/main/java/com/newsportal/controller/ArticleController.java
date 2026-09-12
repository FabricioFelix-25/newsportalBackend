package com.newsportal.controller;
import com.newsportal.dto.ArticleRequest;
import com.newsportal.dto.ArticleResponse;
import com.newsportal.dto.PublishArticleRequest;
import com.newsportal.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<ArticleResponse>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean summary) {
        Pageable pageable = pagination(page, size);
        Page<ArticleResponse> articles = articleService.getAllPublishedArticles(pageable);
        return ResponseEntity.ok(articles.map(article -> summarize(article, summary)));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<ArticleResponse>> getAllArticlesForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = pagination(page, size);
        Page<ArticleResponse> articles = articleService.getAllArticlesForAdmin(pageable);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(articles);
    }

    @GetMapping("/admin/{id}/preview")
    public ResponseEntity<ArticleResponse> previewArticle(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .body(articleService.getArticleForReview(id));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ArticleResponse> publishArticle(
            @PathVariable Long id,
            @Valid @RequestBody PublishArticleRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .body(articleService.publishArticle(id, request));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ArticleResponse>> getFeaturedArticles(
            @RequestParam(defaultValue = "false") boolean summary) {
        List<ArticleResponse> articles = articleService.getFeaturedArticles();
        return ResponseEntity.ok(articles.stream().map(article -> summarize(article, summary)).toList());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ArticleResponse>> getArticlesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean summary) {
        Pageable pageable = pagination(page, size);
        Page<ArticleResponse> articles = articleService.getArticlesByCategory(category, pageable);
        return ResponseEntity.ok(articles.map(article -> summarize(article, summary)));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<ArticleResponse>> getArticlesByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean summary) {
        Pageable pageable = pagination(page, size);
        Page<ArticleResponse> articles = articleService.getArticlesByAuthor(authorId, pageable);
        return ResponseEntity.ok(articles.map(article -> summarize(article, summary)));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleResponse>> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean summary) {
        Pageable pageable = pagination(page, size);
        Page<ArticleResponse> articles = articleService.searchArticles(q, tag, pageable);
        return ResponseEntity.ok(articles.map(article -> summarize(article, summary)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        ArticleResponse article = articleService.getArticleById(id);
        return ResponseEntity.ok(article);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArticleResponse> getArticleBySlug(
            @PathVariable String slug) {
        ArticleResponse article = articleService.getArticleBySlug(slug);
        return ResponseEntity.ok(article);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, String>> trackArticleView(
            @PathVariable Long id,
            HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        articleService.trackView(id, userAgent, ipAddress);
        return ResponseEntity.ok(Map.of("message", "view tracked"));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody ArticleRequest request) {
        ArticleResponse article = articleService.createArticle(request);
        return ResponseEntity.ok(article);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request) {
        ArticleResponse article = articleService.updateArticle(id, request);
        return ResponseEntity.ok(article);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = articleService.getCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        Object stats = articleService.getStats();
        return ResponseEntity.ok(stats);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
        }
    }

    private Pageable pagination(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    private ArticleResponse summarize(ArticleResponse article, boolean summary) {
        if (summary) {
            article.setContent("");
        }
        return article;
    }
}
