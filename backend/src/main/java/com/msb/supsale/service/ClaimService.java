package com.msb.supsale.service;

import com.msb.supsale.model.Claim;
import com.msb.supsale.model.EmailLog;
import com.msb.supsale.repository.ClaimRepository;
import com.msb.supsale.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {
    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository claimRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailService emailService;
    private final CustomerService customerService;
    private final String mailFrom;

    public ClaimService(ClaimRepository claimRepository, EmailLogRepository emailLogRepository,
                        EmailService emailService, CustomerService customerService,
                        @Value("${mail.from:noreply@sup-sale.msb.demo}") String mailFrom) {
        this.claimRepository = claimRepository;
        this.emailLogRepository = emailLogRepository;
        this.emailService = emailService;
        this.customerService = customerService;
        this.mailFrom = mailFrom;
    }

    public List<Claim> getAllClaims() { return claimRepository.findAllByOrderByCreatedAtDesc(); }
    public List<Claim> getClaimsByStatus(String status) { return claimRepository.findByStatusOrderByCreatedAtDesc(status); }
    public Claim getClaim(UUID id) { return claimRepository.findById(id).orElseThrow(); }

    @Transactional
    public Claim createClaim(String sessionId, String customerName, String customerPhone,
                             String topic, String claimContent, String suggestedResponse) {
        Claim claim = new Claim();
        claim.setSessionId(sessionId);
        claim.setCustomerName(customerName);
        claim.setCustomerPhone(customerPhone);
        claim.setTopic(topic);
        claim.setClaimContent(claimContent);
        claim.setSuggestedResponse(suggestedResponse);
        claim.setStatus("PENDING");
        log.info("Claim created: session={}, topic={}", sessionId, topic);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim approveClaim(UUID claimId, UUID resolvedBy, String editedResponse) {
        Claim claim = getClaim(claimId);
        claim.setStatus("APPROVED");
        claim.setResolvedAt(Instant.now());
        claim.setResolvedBy(resolvedBy);

        String response = editedResponse != null && !editedResponse.isBlank() ? editedResponse : claim.getSuggestedResponse();

        String toEmail;
        boolean isMockEmail;
        var customer = customerService.findByPhone(claim.getCustomerPhone());
        if (customer.isPresent() && customer.get().getEmail() != null && !customer.get().getEmail().isBlank()) {
            toEmail = customer.get().getEmail();
            isMockEmail = false;
            log.info("Using real customer email: {}", toEmail);
        } else {
            toEmail = claim.getCustomerPhone() + "@sms.msb.demo";
            isMockEmail = true;
            log.info("No real email, using fallback: {}", toEmail);
        }
        String subject = "Phản hồi khiếu nại từ MSB";

        emailService.send(toEmail, subject, response, isMockEmail);

        EmailLog emailLog = new EmailLog();
        emailLog.setClaimId(claimId);
        emailLog.setToEmail(toEmail);
        emailLog.setSubject(subject);
        emailLog.setBody(response);
        emailLog.setMock(isMockEmail);
        emailLogRepository.save(emailLog);

        log.info("Claim approved: id={}, email sent (mock={})", claimId, isMockEmail);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim abortClaim(UUID claimId, UUID resolvedBy) {
        Claim claim = getClaim(claimId);
        claim.setStatus("ABORTED");
        claim.setResolvedAt(Instant.now());
        claim.setResolvedBy(resolvedBy);
        log.info("Claim aborted: id={}", claimId);
        return claimRepository.save(claim);
    }
}
