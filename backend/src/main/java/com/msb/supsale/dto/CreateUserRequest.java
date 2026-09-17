package com.msb.supsale.dto;

import jakarta.validation.constraints.*;

public class CreateUserRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
    @NotBlank
    private String fullName;
    @NotBlank
    @Pattern(regexp = "ADMIN|STAFF")
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
