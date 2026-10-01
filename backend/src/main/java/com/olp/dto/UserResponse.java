package com.olp.dto;

public class UserResponse {

    private Integer id;
    private String name;
    private String email;
    private String role;

    public UserResponse(Integer id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
