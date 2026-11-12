package com.olp.dto;

public class UserResponse {

    private Integer id;
    private String name;
    private String email;
    private String role;
    private String accountStatus;

    public UserResponse() {}

    public UserResponse(Integer id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public UserResponse(Integer id, String name, String email, String role, String accountStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    // ===== Getters and Setters =====

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
}
