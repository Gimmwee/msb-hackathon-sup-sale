package com.msb.supsale.dto;

import java.time.Instant;
import java.util.UUID;

public class LeadDto {
    private UUID id;
    private String sessionId;
    private String customerName;
    private String phone;
    private String productInterest;
    private String status;
    private Instant createdAt;
    private String idNumber;
    private String dob;
    private String gender;
    private String address;

    public LeadDto() {}
    public LeadDto(com.msb.supsale.model.Lead lead) {
        this.id = lead.getId(); this.sessionId = lead.getSessionId();
        this.customerName = lead.getCustomerName(); this.phone = lead.getPhone();
        this.productInterest = lead.getProductInterest(); this.status = lead.getStatus();
        this.createdAt = lead.getCreatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProductInterest() { return productInterest; }
    public void setProductInterest(String productInterest) { this.productInterest = productInterest; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
