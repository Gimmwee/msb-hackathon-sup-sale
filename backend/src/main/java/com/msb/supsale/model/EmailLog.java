package com.msb.supsale.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_log")
public class EmailLog {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "sent_at", updatable = false)
    private Instant sentAt;

    private boolean mock;

    @PrePersist
    void prePersist() { sentAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getClaimId() { return claimId; }
    public void setClaimId(UUID claimId) { this.claimId = claimId; }
    public String getToEmail() { return toEmail; }
    public void setToEmail(String toEmail) { this.toEmail = toEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getSentAt() { return sentAt; }
    public boolean isMock() { return mock; }
    public void setMock(boolean mock) { this.mock = mock; }
}
