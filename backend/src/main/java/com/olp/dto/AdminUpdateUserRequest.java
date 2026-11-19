package com.olp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for admin-initiated user detail updates (FR-A02, ADM-022).
 */
public class AdminUpdateUserRequest {

    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must be 255 characters or fewer")
    private String email;

    private String role;

    public AdminUpdateUserRequest() {
    }

    public AdminUpdateUserRequest(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}