package com.newsportal.service;

import com.newsportal.dto.ArticleRequest;
import com.newsportal.dto.ArticleResponse;
import com.newsportal.exception.BadRequestException;
import com.newsportal.exception.ResourceNotFoundException;
import com.newsportal.model.Article;
import com.newsportal.model.Author;
import com.newsportal.repository.ArticleRepository;
import com.newsportal.repository.ArticleViewRepository;
import com.newsportal.repository.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private ArticleViewRepository articleViewRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void createArticleShouldRejectPublishWhenChecklistIsIncomplete() {
        ArticleRequest request = baseRequest();
        request.setIsDraft(false);
        request.setSourceReferences(null);
        request.setReviewedBy("Editor");
        request.setFactChecked(true);
        request.setRightsCleared(true);
        request.setSensitiveContentReviewed(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> articleService.createArticle(request));
        assertEquals("Source references are required before publishing", ex.getMessage());
    }

    @Test
    void createArticleShouldCreateDraftAndGenerateSlug() {
        ArticleRequest request = baseRequest();
        request.setTitle("Hello News Portal!");
        request.setIsDraft(true);

        Author author = new Author();
        author.setId(1L);
        author.setName("Author");
        author.setEmail("author@mail.com");

        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(articleRepository.existsBySlug("hello-news-portal")).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        ArticleResponse response = articleService.createArticle(request);

        assertEquals(100L, response.getId());
        assertEquals("hello-news-portal", response.getSlug());
        assertEquals("tech", response.getCategory());
        assertEquals(true, response.getIsDraft());
    }

    @Test
    void getArticleBySlugShouldThrowWhenDraftIsHidden() {
        Article draft = new Article();
        draft.setId(5L);
        draft.setSlug("draft-post");
        draft.setTitle("Draft");
        draft.setCategory(Article.Category.TECH);
        draft.setAuthor(new Author("A", "a@a.com", null, null));
        draft.setIsDraft(true);

        when(articleRepository.findBySlug("draft-post")).thenReturn(Optional.of(draft));

        assertThrows(ResourceNotFoundException.class, () -> articleService.getArticleBySlug("draft-post", false));
    }

    @Test
    void trackViewShouldPersistViewAndIncrementCounter() {
        Article article = new Article();
        article.setId(10L);
        article.setSlug("a");
        article.setTitle("T");
        article.setCategory(Article.Category.TECH);
        article.setAuthor(new Author("A", "a@a.com", null, null));
        article.setViewCount(7L);

        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        articleService.trackView(10L, "Mozilla", "127.0.0.1");

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(captor.capture());
        assertEquals(8L, captor.getValue().getViewCount());
        verify(articleViewRepository).save(any());
    }

    @Test
    void createArticleShouldRejectInvalidCategory() {
        ArticleRequest request = baseRequest();
        request.setIsDraft(true);
        request.setCategory("not-a-category");

        Author author = new Author();
        author.setId(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThrows(BadRequestException.class, () -> articleService.createArticle(request));
    }

    private ArticleRequest baseRequest() {
        ArticleRequest request = new ArticleRequest();
        request.setTitle("Title");
        request.setContent("Content");
        request.setExcerpt("Excerpt");
        request.setCategory("tech");
        request.setAuthorId(1L);
        request.setTags(Set.of("news"));
        request.setFeatured(false);
        return request;
    }
}

