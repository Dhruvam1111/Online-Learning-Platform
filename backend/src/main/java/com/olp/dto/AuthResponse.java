package com.olp.dto;

public class AuthResponse {

    private String message;
    private String token;
    private UserResponse user;

    public AuthResponse(String message, String token, UserResponse user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // ===== Getters =====

    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserResponse getUser() { return user; }
}
