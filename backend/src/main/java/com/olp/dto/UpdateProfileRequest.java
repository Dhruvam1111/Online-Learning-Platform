package com.olp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;

    @Email(message = "Email must be a valid email address")
    private String email;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // ===== Getters and Setters =====

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
