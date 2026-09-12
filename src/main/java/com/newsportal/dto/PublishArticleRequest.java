package com.newsportal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Apenas decisões editoriais: o texto e as imagens já revisados não são reenviados. */
public record PublishArticleRequest(
        @NotBlank(message = "Source references are required before publishing") String sourceReferences,
        @NotBlank(message = "Reviewer name is required before publishing") @Size(max = 255) String reviewedBy,
        @NotNull @AssertTrue(message = "Fact check must be confirmed before publishing") Boolean factChecked,
        @NotNull @AssertTrue(message = "Image/source rights must be confirmed before publishing") Boolean rightsCleared,
        @NotNull @AssertTrue(message = "Sensitive content review must be confirmed before publishing") Boolean sensitiveContentReviewed
) {}
