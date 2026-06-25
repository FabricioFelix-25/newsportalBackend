package com.newsportal.service;

import com.newsportal.dto.AuthenticatedUserResponse;
import com.newsportal.dto.LoginRequest;
import com.newsportal.dto.LoginResponse;
import com.newsportal.dto.RegisterRequest;
import com.newsportal.exception.ResourceNotFoundException;
import com.newsportal.exception.BadRequestException;
import com.newsportal.model.User;
import com.newsportal.repository.UserRepository;
import com.newsportal.security.JwtService;
import com.newsportal.security.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Value("${app.auth.debug-reset-token:false}")
    private boolean debugResetToken;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String emailKey = request.getEmail().trim().toLowerCase();
        if (loginAttemptService.isBlocked(emailKey)) {
            long waitSeconds = loginAttemptService.getRemainingBlockSeconds(emailKey);
            throw new BadRequestException("Too many login attempts. Try again in " + Math.max(waitSeconds, 1) + " seconds");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginAttemptService.onFailure(emailKey);
                    return new BadRequestException("Invalid email or password");
                });

        if (!user.getIsActive()) {
            throw new BadRequestException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.onFailure(emailKey);
            throw new BadRequestException("Invalid email or password");
        }
        loginAttemptService.onSuccess(emailKey);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new LoginResponse(user.getId(), user.getName(),
                user.getEmail(), user.getRole(), user.getAvatarUrl(),
                token, "Bearer", jwtService.extractExpiration(token).toInstant().getEpochSecond());
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() == null ? User.Role.EDITOR : request.getRole());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setIsActive(true);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return new AuthenticatedUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl()
        );
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Avoid user enumeration: always return success for unknown emails
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        userRepository.save(user);

        if (debugResetToken) {
            System.out.println("Reset token for " + email + ": " + resetToken);
        }
    }

    public void resetPassword(String token, String newPassword) {
        validatePasswordStrength(newPassword);

        User user = userRepository.findByValidResetToken(token, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("Password must have at least 8 chars, one uppercase, one lowercase and one number");
        }
    }
}
