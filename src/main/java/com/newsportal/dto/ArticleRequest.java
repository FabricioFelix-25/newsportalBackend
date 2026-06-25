package com.newsportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class ArticleRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Excerpt is required")
    private String excerpt;

    private String imageUrl;

    @NotBlank(message = "Category is required")
    private String category;

    private Set<String> tags;

    @NotNull(message = "Author ID is required")
    private Long authorId;

    private Boolean featured = false;
    private Boolean isDraft = true;
    private String seoTitle;
    private String seoDescription;
    private String seoImage;
    private Boolean aiAssisted = false;
    private String sourceReferences;
    private String reviewedBy;
    private Boolean factChecked = false;
    private Boolean rightsCleared = false;
    private Boolean sensitiveContentReviewed = false;

    // Constructors
    public ArticleRequest() {}

    // Getters and Setters
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

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

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
}
