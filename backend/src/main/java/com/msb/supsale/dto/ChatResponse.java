package com.msb.supsale.dto;

public class ChatResponse {
    private String sessionId;
    private String message;
    private String intent;
    private boolean leadCaptured;

    public ChatResponse() {}
    public ChatResponse(String sessionId, String message, String intent, boolean leadCaptured) {
        this.sessionId = sessionId; this.message = message; this.intent = intent; this.leadCaptured = leadCaptured;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public boolean isLeadCaptured() { return leadCaptured; }
    public void setLeadCaptured(boolean leadCaptured) { this.leadCaptured = leadCaptured; }
}
