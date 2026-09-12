package com.newsportal.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAuthenticationFilterTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validApiKeyGrantsOnlyBotRole() throws Exception {
        filter("local-test-key", "local-test-key", null);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("AI_BOT", authentication.getName());
        assertEquals(1, authentication.getAuthorities().size());
        assertEquals("ROLE_BOT", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void blankOrIncorrectKeyNeverAuthenticates() throws Exception {
        for (String configured : new String[]{null, "", "   ", "correct-key"}) {
            filter(configured, configured == null ? "" : configured.equals("correct-key") ? "wrong-key" : configured, null);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Test
    void authorizationHeaderPreventsApiKeyFromMaskingJwtAuthentication() throws Exception {
        filter("local-test-key", "local-test-key", "Bearer delegated-to-jwt-filter");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void filter(String configuredKey, String suppliedKey, String authorization) throws Exception {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "configuredApiKey", configuredKey);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", suppliedKey);
        if (authorization != null) request.addHeader("Authorization", authorization);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
}
