package com.newsportal.controller;
import com.newsportal.dto.ArticleRequest;
import com.newsportal.dto.ArticleResponse;
import com.newsportal.model.Article;
import com.newsportal.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleResponse> articles = articleService.getAllPublishedArticles(pageable);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<ArticleResponse>> getAllArticlesForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleResponse> articles = articleService.getAllArticlesForAdmin(pageable);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ArticleResponse>> getFeaturedArticles() {
        List<ArticleResponse> articles = articleService.getFeaturedArticles();
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ArticleResponse>> getArticlesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleResponse> articles = articleService.getArticlesByCategory(category, pageable);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<ArticleResponse>> getArticlesByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleResponse> articles = articleService.getArticlesByAuthor(authorId, pageable);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleResponse>> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleResponse> articles = articleService.searchArticles(q, tag, pageable);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(
            @PathVariable Long id,
            Authentication authentication) {
        ArticleResponse article = articleService.getArticleById(id, isPrivilegedUser(authentication));
        return ResponseEntity.ok(article);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArticleResponse> getArticleBySlug(
            @PathVariable String slug,
            Authentication authentication,
            HttpServletRequest request) {
        ArticleResponse article = articleService.getArticleBySlug(slug, isPrivilegedUser(authentication));

        // Track view
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);
        articleService.trackView(article.getId(), userAgent, ipAddress);

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

    private boolean isPrivilegedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority)
                                || "ROLE_EDITOR".equals(authority)
                                || "ROLE_AUTHOR".equals(authority)
                );
    }
}
