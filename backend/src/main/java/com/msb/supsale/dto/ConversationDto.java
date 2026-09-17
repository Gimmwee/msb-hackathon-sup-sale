package com.msb.supsale.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ConversationDto {
    private UUID id;
    private String sessionId;
    private String platform;
    private Instant createdAt;
    private List<MessageDto> messages;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<MessageDto> getMessages() { return messages; }
    public void setMessages(List<MessageDto> messages) { this.messages = messages; }

    public static class MessageDto {
        private String role;
        private String content;
        private Instant createdAt;

        public MessageDto() {}
        public MessageDto(com.msb.supsale.model.Message msg) {
            this.role = msg.getRole(); this.content = msg.getContent();
            this.createdAt = msg.getCreatedAt();
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
