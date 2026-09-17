package com.msb.supsale.dto;

public class LoginResponse {
    private String accessToken;
    private long expiresIn;
    private String role;
    private String fullName;

    public LoginResponse(String accessToken, long expiresIn, String role, String fullName) {
        this.accessToken = accessToken; this.expiresIn = expiresIn;
        this.role = role; this.fullName = fullName;
    }

    public String getAccessToken() { return accessToken; }
    public long getExpiresIn() { return expiresIn; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
}
