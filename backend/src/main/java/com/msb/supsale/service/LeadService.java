package com.msb.supsale.service;

import com.msb.supsale.model.Lead;
import com.msb.supsale.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LeadService {
    private static final Logger log = LoggerFactory.getLogger(LeadService.class);
    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Transactional
    public Lead captureLead(String sessionId, String customerName, String phone, String productInterest) {
        Optional<Lead> existing = leadRepository.findBySessionIdAndPhone(sessionId, phone);
        if (existing.isPresent()) {
            Lead lead = existing.get();
            lead.setCustomerName(customerName);
            lead.setProductInterest(productInterest);
            log.info("Lead updated: session={}, phone={}", sessionId, phone);
            return leadRepository.save(lead);
        }
        Lead lead = new Lead();
        lead.setSessionId(sessionId);
        lead.setCustomerName(customerName);
        lead.setPhone(phone);
        lead.setProductInterest(productInterest);
        lead.setStatus("NEW");
        log.info("Lead captured: session={}, phone={}, name={}", sessionId, phone, customerName);
        return leadRepository.save(lead);
    }

    public List<Lead> getAllLeads() {
        return leadRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countTotal() { return leadRepository.count(); }
    public long countByStatus(String status) { return leadRepository.countByStatus(status); }

    @Transactional
    public Lead updateStatus(java.util.UUID leadId, String status) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found: " + leadId));
        lead.setStatus(status);
        log.info("Lead status updated: id={}, status={}", leadId, status);
        return leadRepository.save(lead);
    }
}
