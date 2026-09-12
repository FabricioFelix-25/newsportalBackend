package com.newsportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsportal.model.Article;
import com.newsportal.model.Author;
import com.newsportal.model.User;
import com.newsportal.repository.ArticleRepository;
import com.newsportal.repository.ArticleViewRepository;
import com.newsportal.repository.AuthorRepository;
import com.newsportal.repository.UserRepository;
import com.newsportal.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
class ArticleSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ArticleRepository articles;
    @Autowired ArticleViewRepository views;
    @Autowired AuthorRepository authors;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;

    private Author author;
    private Article draft;
    private Article published;
    private String adminToken;
    private String editorToken;
    private String authorToken;

    @BeforeEach
    void seedLocalDatabase() {
        views.deleteAll();
        articles.deleteAll();
        authors.deleteAll();
        users.deleteAll();
        author = authors.save(new Author("Redação", "redacao@example.test", null, null));
        draft = article("rascunho-secreto", true, "segredo");
        published = article("noticia-publica", false, "publico");
        adminToken = token(User.Role.ADMIN);
        editorToken = token(User.Role.EDITOR);
        authorToken = token(User.Role.AUTHOR);
    }

    @Test
    void publicRoutesNeverExposeDraftEvenWithEditorialAuthentication() throws Exception {
        for (String path : new String[]{"/articles/" + draft.getId(), "/articles/slug/" + draft.getSlug()}) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
            mvc.perform(get(path).header("Authorization", adminToken)).andExpect(status().isNotFound());
            mvc.perform(get(path).header("X-API-KEY", "local-test-bot-key")).andExpect(status().isNotFound());
        }
        mvc.perform(post("/articles/{id}/view", draft.getId())).andExpect(status().isNotFound());
        assertEquals(0L, views.count());
        assertEquals(0L, articles.findById(draft.getId()).orElseThrow().getViewCount());
    }

    @Test
    void nullDraftStatusIsNotTreatedAsPublished() throws Exception {
        draft.setIsDraft(null);
        articles.save(draft);
        mvc.perform(get("/articles/{id}", draft.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/articles/slug/{slug}", draft.getSlug())).andExpect(status().isNotFound());
        mvc.perform(post("/articles/{id}/view", draft.getId())).andExpect(status().isNotFound());
    }

    @Test
    void allPublicCollectionsFilterDraftsIncludingFeaturedSearchAndTags() throws Exception {
        for (String path : new String[]{"/articles", "/articles/category/tech", "/articles/author/" + author.getId(),
                "/articles/search?q=noticia", "/articles/search?tag=publico"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(published.getId()));
        }
        mvc.perform(get("/articles/featured")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(published.getId()));
        for (String path : new String[]{"/articles/search?q=rascunho-secreto", "/articles/search?tag=segredo"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void privatePreviewRequiresActiveEditorialJwtAndNeverTracksViews() throws Exception {
        String path = "/articles/admin/" + draft.getId() + "/preview";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", authorToken)).andExpect(status().isForbidden());
        mvc.perform(get(path).header("X-API-KEY", "local-test-bot-key")).andExpect(status().isForbidden());
        for (String token : new String[]{adminToken, editorToken}) {
            mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(header().string("Cache-Control", containsString("private")))
                    .andExpect(header().string("X-Robots-Tag", containsString("noindex")))
                    .andExpect(jsonPath("$.content").value(draft.getContent()))
                    .andExpect(jsonPath("$.isDraft").value(true));
        }
        assertEquals(0L, views.count());
        mvc.perform(get(path).header("Authorization", editorToken).header("X-API-KEY", "local-test-bot-key"))
                .andExpect(status().isOk());
        User editor = users.findByEmail("editor@example.test").orElseThrow();
        editor.setIsActive(false);
        users.save(editor);
        mvc.perform(get(path).header("Authorization", editorToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminListingIsPrivateAndRoleRestricted() throws Exception {
        mvc.perform(get("/articles/admin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/articles/admin").header("X-API-KEY", "local-test-bot-key")).andExpect(status().isForbidden());
        mvc.perform(get("/articles/admin").header("Authorization", editorToken)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void publishPreservesReviewedContentAndUsesActualPublicationDate() throws Exception {
        LocalDateTime beforePublication = LocalDateTime.now().minusSeconds(1);
        Map<String, Object> checklist = validChecklist();
        checklist.put("content", "This field must never overwrite the reviewed article");
        mvc.perform(post("/articles/{id}/publish", draft.getId()).header("Authorization", editorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(checklist)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isDraft").value(false))
                .andExpect(jsonPath("$.content").value(draft.getContent()));
        Article saved = articles.findById(draft.getId()).orElseThrow();
        assertTrue(saved.getPublishedAt().isAfter(beforePublication));
        assertEquals(draft.getTitle(), saved.getTitle());
        assertEquals(draft.getImageUrl(), saved.getImageUrl());
        assertEquals(draft.getSlug(), saved.getSlug());
        assertEquals(draft.getTags(), saved.getTags());
        assertTrue(saved.getAiAssisted());
        LocalDateTime firstPublishedAt = saved.getPublishedAt();
        mvc.perform(post("/articles/{id}/publish", draft.getId()).header("Authorization", editorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(validChecklist())))
                .andExpect(status().isOk());
        assertEquals(firstPublishedAt, articles.findById(draft.getId()).orElseThrow().getPublishedAt());
        mvc.perform(get("/articles/slug/{slug}", draft.getSlug())).andExpect(status().isOk());
    }

    @Test
    void publishRejectsIncompleteChecklistAndNonEditorialCredentials() throws Exception {
        String path = "/articles/" + draft.getId() + "/publish";
        String valid = json.writeValueAsString(validChecklist());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("X-API-KEY", "local-test-bot-key")
                .contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", authorToken)
                .contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isForbidden());
        for (String field : new String[]{"factChecked", "rightsCleared", "sensitiveContentReviewed"}) {
            Map<String, Object> checklist = validChecklist();
            checklist.put(field, false);
            mvc.perform(post(path).header("Authorization", editorToken)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(checklist)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(path).header("Authorization", editorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertTrue(articles.findById(draft.getId()).orElseThrow().getIsDraft());
    }

    @Test
    void botCanCreateOnlyDraftsAndCannotModifyExistingArticles() throws Exception {
        Map<String, Object> body = articleBody();
        mvc.perform(post("/articles").header("X-API-KEY", "local-test-bot-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isDraft").value(true));
        body.putAll(validChecklist());
        body.put("isDraft", false);
        mvc.perform(post("/articles").header("X-API-KEY", "local-test-bot-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/articles").header("Authorization", authorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/articles/{id}", published.getId()).header("X-API-KEY", "local-test-bot-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/articles/{id}", draft.getId()).header("X-API-KEY", "local-test-bot-key"))
                .andExpect(status().isForbidden());
        body.put("isDraft", null);
        mvc.perform(post("/articles").header("X-API-KEY", "local-test-bot-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        assertEquals(3L, articles.count());
    }

    @Test
    void publicReadHasNoWriteAndExplicitViewDoesNotChangeEditorialTimestamp() throws Exception {
        LocalDateTime originalUpdatedAt = articles.findById(published.getId()).orElseThrow().getUpdatedAt();
        mvc.perform(get("/articles/slug/{slug}", published.getSlug())).andExpect(status().isOk());
        assertEquals(0L, views.count());
        mvc.perform(post("/articles/{id}/view", published.getId())).andExpect(status().isOk());
        Article saved = articles.findById(published.getId()).orElseThrow();
        assertEquals(1L, saved.getViewCount());
        assertEquals(originalUpdatedAt, saved.getUpdatedAt());
        assertEquals(1L, views.count());
    }

    @Test
    void summaryKeepsCardFieldsAndDefaultStillReturnsCompleteContent() throws Exception {
        mvc.perform(get("/articles?summary=true")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value(""))
                .andExpect(jsonPath("$.content[0].title").value(published.getTitle()))
                .andExpect(jsonPath("$.content[0].author.name").value(author.getName()));
        mvc.perform(get("/articles/featured?summary=true")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value(""));
        mvc.perform(get("/articles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value(published.getContent()));
        mvc.perform(get("/articles?page=-1&size=100000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100)).andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void anonymousAndBotCannotRegisterAnAdministrator() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "Injected Admin", "email", "other@example.test",
                "password", "StrongLocal123", "role", "ADMIN"));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/register").header("X-API-KEY", "local-test-bot-key")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/register").header("Authorization", editorToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        assertFalse(users.existsByEmail("other@example.test"));
    }

    private Article article(String slug, boolean isDraft, String tag) {
        Article article = new Article(slug, slug, "<p>Texto integral " + slug + "</p>", "Resumo da noticia",
                "https://example.test/editorial.jpg", Article.Category.TECH, author);
        article.setIsDraft(isDraft);
        article.setFeatured(true);
        article.setAiAssisted(true);
        article.setTags(Set.of(tag));
        article.setPublishedAt(LocalDateTime.now().minusDays(3));
        return articles.save(article);
    }

    private String token(User.Role role) {
        User user = users.save(new User(role.name(), role.name().toLowerCase() + "@example.test", "test-only", role));
        return "Bearer " + jwt.generateToken(user);
    }

    private Map<String, Object> validChecklist() {
        return new LinkedHashMap<>(Map.of("sourceReferences", "https://example.test/source", "reviewedBy", "Editor",
                "factChecked", true, "rightsCleared", true, "sensitiveContentReviewed", true));
    }

    private Map<String, Object> articleBody() {
        return new LinkedHashMap<>(Map.of("title", "Rascunho enviado pelo bot", "content", "<p>Texto</p>",
                "excerpt", "Resumo", "category", "tech", "authorId", author.getId(), "isDraft", true));
    }
}
