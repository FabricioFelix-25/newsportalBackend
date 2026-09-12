package com.newsportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${app.ai.api-key:#{null}}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (SecurityContextHolder.getContext().getAuthentication() == null
                && request.getHeader("Authorization") == null
                && requestApiKey != null && configuredApiKey != null && !configuredApiKey.isBlank()
                && MessageDigest.isEqual(requestApiKey.getBytes(StandardCharsets.UTF_8),
                    configuredApiKey.getBytes(StandardCharsets.UTF_8))) {
            // O bot pode enviar rascunhos; revisão e publicação exigem uma conta editorial.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "AI_BOT", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_BOT")));
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
