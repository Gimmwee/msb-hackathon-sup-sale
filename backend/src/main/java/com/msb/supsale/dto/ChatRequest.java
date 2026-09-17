package com.msb.supsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {
    @NotBlank
    private String sessionId;
    private String platform = "WEB";
    @NotBlank
    @Size(max = 2000)
    private String message;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
