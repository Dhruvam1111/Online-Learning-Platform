package com.onlinelearning.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {

    @NotBlank(message = "Google ID token cannot be empty")
    private String idToken;

    private String role; // Optional: student or instructor. Defaults to student if not provided for new accounts.

    public GoogleAuthRequest() {
    }

    public GoogleAuthRequest(String idToken, String role) {
        this.idToken = idToken;
        this.role = role;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
