package com.msb.supsale.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cic_lookup_logs")
public class CicLookupLog {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "staff_user_id", nullable = false)
    private UUID staffUserId;

    @Column(name = "queried_value", nullable = false)
    private String queriedValue;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getStaffUserId() { return staffUserId; }
    public void setStaffUserId(UUID staffUserId) { this.staffUserId = staffUserId; }
    public String getQueriedValue() { return queriedValue; }
    public void setQueriedValue(String queriedValue) { this.queriedValue = queriedValue; }
    public Instant getCreatedAt() { return createdAt; }
}
