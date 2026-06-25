package com.newsportal.dto;

import com.newsportal.model.User;

public class LoginResponse {
    private Long id;
    private String name;
    private String email;
    private User.Role role;
    private String avatarUrl;
    private String token;
    private String tokenType;
    private Long expiresAtEpochSeconds;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(Long id, String name, String email, User.Role role, String avatarUrl,
                         String token, String tokenType, Long expiresAtEpochSeconds) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.token = token;
        this.tokenType = tokenType;
        this.expiresAtEpochSeconds = expiresAtEpochSeconds;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getExpiresAtEpochSeconds() { return expiresAtEpochSeconds; }
    public void setExpiresAtEpochSeconds(Long expiresAtEpochSeconds) { this.expiresAtEpochSeconds = expiresAtEpochSeconds; }
}
