package com.olp.dto;

public class AuthResponse {

    private String message;
    private String token;
    private UserResponse user;
    private String verificationLink;

    public AuthResponse() {}

    public AuthResponse(String message, String token, UserResponse user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }

    public AuthResponse(String message, String token, UserResponse user, String verificationLink) {
        this.message = message;
        this.token = token;
        this.user = user;
        this.verificationLink = verificationLink;
    }

    // ===== Getters and Setters =====

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public String getVerificationLink() {
        return verificationLink;
    }

    public void setVerificationLink(String verificationLink) {
        this.verificationLink = verificationLink;
    }
}
