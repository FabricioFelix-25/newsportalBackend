package com.newsportal.security;

import com.newsportal.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void generateAndValidateTokenShouldWorkForExpectedUser() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "123456789012345678901234567890123456789012345678");
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", 60L);

        User user = new User();
        user.setId(77L);
        user.setEmail("user@mail.com");
        user.setRole(User.Role.EDITOR);

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertEquals("user@mail.com", jwtService.extractUsername(token));
        assertNotNull(jwtService.extractExpiration(token));

        UserDetails matching = org.springframework.security.core.userdetails.User
                .withUsername("user@mail.com")
                .password("x")
                .authorities("ROLE_EDITOR")
                .build();

        assertTrue(jwtService.isTokenValid(token, matching));
    }

    @Test
    void tokenShouldBeInvalidForDifferentUser() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "123456789012345678901234567890123456789012345678");
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", 60L);

        User user = new User();
        user.setId(88L);
        user.setEmail("owner@mail.com");
        user.setRole(User.Role.ADMIN);

        String token = jwtService.generateToken(user);

        UserDetails anotherUser = org.springframework.security.core.userdetails.User
                .withUsername("other@mail.com")
                .password("x")
                .authorities("ROLE_ADMIN")
                .build();

        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }
}

