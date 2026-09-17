package com.msb.supsale.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cic_records")
public class CicRecord {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "id_number", nullable = false)
    private String idNumber;

    private String phone;

    @Column(name = "credit_score", nullable = false)
    private int creditScore;

    @Column(name = "debt_group", nullable = false)
    private String debtGroup;

    @Column(name = "outstanding_loans", nullable = false)
    private long outstandingLoans;

    @Column(name = "last_updated", updatable = false)
    private Instant lastUpdated;

    @PrePersist
    void prePersist() { lastUpdated = Instant.now(); }

    public UUID getId() { return id; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }
    public String getDebtGroup() { return debtGroup; }
    public void setDebtGroup(String debtGroup) { this.debtGroup = debtGroup; }
    public long getOutstandingLoans() { return outstandingLoans; }
    public void setOutstandingLoans(long outstandingLoans) { this.outstandingLoans = outstandingLoans; }
    public Instant getLastUpdated() { return lastUpdated; }
}
