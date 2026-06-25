package com.newsportal.dto;
import com.newsportal.model.Article;

import java.time.LocalDateTime;
import java.util.Set;

public class ArticleResponse {
    private Long id;
    private String slug;
    private String title;
    private String subtitle;
    private String content;
    private String excerpt;
    private String imageUrl;
    private String category;
    private Set<String> tags;
    private AuthorResponse author;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private Boolean featured;
    private Boolean isDraft;
    private String seoTitle;
    private String seoDescription;
    private String seoImage;
    private Boolean aiAssisted;
    private String sourceReferences;
    private String reviewedBy;
    private Boolean factChecked;
    private Boolean rightsCleared;
    private Boolean sensitiveContentReviewed;
    private Long viewCount;

    // Constructors
    public ArticleResponse() {}

    public ArticleResponse(Article article) {
        this.id = article.getId();
        this.slug = article.getSlug();
        this.title = article.getTitle();
        this.subtitle = article.getSubtitle();
        this.content = article.getContent();
        this.excerpt = article.getExcerpt();
        this.imageUrl = article.getImageUrl();
        this.category = article.getCategory().name().toLowerCase().replace('_', '-');
        this.tags = article.getTags();
        this.author = new AuthorResponse(article.getAuthor());
        this.publishedAt = article.getPublishedAt();
        this.updatedAt = article.getUpdatedAt();
        this.featured = article.getFeatured();
        this.isDraft = article.getIsDraft();
        this.seoTitle = article.getSeoTitle();
        this.seoDescription = article.getSeoDescription();
        this.seoImage = article.getSeoImage();
        this.aiAssisted = article.getAiAssisted();
        this.sourceReferences = article.getSourceReferences();
        this.reviewedBy = article.getReviewedBy();
        this.factChecked = article.getFactChecked();
        this.rightsCleared = article.getRightsCleared();
        this.sensitiveContentReviewed = article.getSensitiveContentReviewed();
        this.viewCount = article.getViewCount();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public AuthorResponse getAuthor() { return author; }
    public void setAuthor(AuthorResponse author) { this.author = author; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }

    public Boolean getIsDraft() { return isDraft; }
    public void setIsDraft(Boolean isDraft) { this.isDraft = isDraft; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public String getSeoImage() { return seoImage; }
    public void setSeoImage(String seoImage) { this.seoImage = seoImage; }

    public Boolean getAiAssisted() { return aiAssisted; }
    public void setAiAssisted(Boolean aiAssisted) { this.aiAssisted = aiAssisted; }

    public String getSourceReferences() { return sourceReferences; }
    public void setSourceReferences(String sourceReferences) { this.sourceReferences = sourceReferences; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Boolean getFactChecked() { return factChecked; }
    public void setFactChecked(Boolean factChecked) { this.factChecked = factChecked; }

    public Boolean getRightsCleared() { return rightsCleared; }
    public void setRightsCleared(Boolean rightsCleared) { this.rightsCleared = rightsCleared; }

    public Boolean getSensitiveContentReviewed() { return sensitiveContentReviewed; }
    public void setSensitiveContentReviewed(Boolean sensitiveContentReviewed) { this.sensitiveContentReviewed = sensitiveContentReviewed; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
}
