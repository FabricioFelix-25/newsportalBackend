package com.newsportal.service;

import com.newsportal.dto.LoginRequest;
import com.newsportal.dto.LoginResponse;
import com.newsportal.dto.RegisterRequest;
import com.newsportal.exception.BadRequestException;
import com.newsportal.model.User;
import com.newsportal.repository.UserRepository;
import com.newsportal.security.JwtService;
import com.newsportal.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtService jwtService;

    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUpJwtService() {
        jwtService = new JwtService();
        loginAttemptService = new LoginAttemptService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "123456789012345678901234567890123456789012345678");
        ReflectionTestUtils.setField(jwtService, "expirationMinutes", 60L);
        ReflectionTestUtils.setField(authService, "jwtService", jwtService);
        ReflectionTestUtils.setField(authService, "loginAttemptService", loginAttemptService);
        ReflectionTestUtils.setField(authService, "debugResetToken", false);
    }

    @Test
    void loginShouldRejectWhenUserIsBlocked() {
        LoginRequest request = new LoginRequest("USER@MAIL.COM", "Pass1234");
        String key = "user@mail.com";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.onFailure(key);
        }

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Too many login attempts."));
    }

    @Test
    void loginShouldRegisterFailureWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("user@mail.com", "bad-pass");
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void loginShouldReturnTokenAndPersistLastLoginWhenCredentialsAreValid() {
        User user = buildUser();
        LoginRequest request = new LoginRequest("user@mail.com", "Pass1234");

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass1234", "encoded")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.login(request);

        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(user.getId(), response.getId());
        assertNotNull(user.getLastLogin());
    }

    @Test
    void registerShouldRejectWeakPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("User");
        request.setEmail("user@mail.com");
        request.setPassword("123");

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertEquals("Password must have at least 8 chars, one uppercase, one lowercase and one number", ex.getMessage());
    }

    @Test
    void registerShouldPersistEditorRoleByDefault() {
        RegisterRequest request = new RegisterRequest();
        request.setName("User");
        request.setEmail("user@mail.com");
        request.setPassword("Strong123");
        request.setRole(null);

        when(userRepository.existsByEmail("user@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Strong123")).thenReturn("hashed");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(User.Role.EDITOR, saved.getRole());
        assertEquals(Boolean.TRUE, saved.getIsActive());
        assertEquals("hashed", saved.getPassword());
    }

    @Test
    void forgotPasswordShouldDoNothingWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        authService.forgotPassword("missing@mail.com");

        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void resetPasswordShouldEncodeAndClearResetToken() {
        User user = buildUser();
        user.setResetToken("token-reset");
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));

        when(userRepository.findByValidResetToken(eq("token-reset"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Strong123")).thenReturn("new-hash");

        authService.resetPassword("token-reset", "Strong123");

        assertEquals("new-hash", user.getPassword());
        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpiry());
        org.mockito.Mockito.verify(userRepository).save(user);
    }

    private User buildUser() {
        User user = new User();
        user.setId(10L);
        user.setName("Test User");
        user.setEmail("user@mail.com");
        user.setPassword("encoded");
        user.setRole(User.Role.EDITOR);
        user.setIsActive(true);
        return user;
    }
}
