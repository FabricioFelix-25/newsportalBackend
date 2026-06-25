package com.newsportal.dto;

import com.newsportal.model.User;

public class AuthenticatedUserResponse {
    private Long id;
    private String name;
    private String email;
    private User.Role role;
    private String avatarUrl;

    public AuthenticatedUserResponse() {
    }

    public AuthenticatedUserResponse(Long id, String name, String email, User.Role role, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
