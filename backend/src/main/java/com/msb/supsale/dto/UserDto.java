package com.msb.supsale.dto;

import java.time.Instant;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String username;
    private String fullName;
    private String role;
    private boolean active;
    private Instant createdAt;

    public UserDto() {}
    public UserDto(com.msb.supsale.model.User user) {
        this.id = user.getId(); this.username = user.getUsername();
        this.fullName = user.getFullName(); this.role = user.getRole().name();
        this.active = user.isActive(); this.createdAt = user.getCreatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
