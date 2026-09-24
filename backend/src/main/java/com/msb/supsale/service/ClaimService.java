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
        String customerName = claim.getCustomerName() != null ? claim.getCustomerName() : "Quý khách";
        String claimRef = claimId.toString().substring(0, 8).toUpperCase();
        String topic = claim.getTopic() != null ? claim.getTopic() : "Khiếu nại";

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

        String subject = "[MSB] Xac nhan khieu nai — " + topic + " — Khach hang: " + customerName + " — Ma #" + claimRef;

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Kinh gui: ").append(customerName).append(",\n\n");
        bodyBuilder.append("MSB Bank xin chan thanh cam on Anh/Chi da phan hoi va gui khieu nai den chung toi.\n\n");
        bodyBuilder.append("═══════════════════════════════════════\n");
        bodyBuilder.append("THONG TIN KHIEU NAI\n");
        bodyBuilder.append("═══════════════════════════════════════\n");
        bodyBuilder.append("Ma khiếu nai: #").append(claimRef).append("\n");
        bodyBuilder.append("Chu de: ").append(topic).append("\n");
        if (claim.getCustomerPhone() != null) {
            bodyBuilder.append("So dien thoai: ").append(claim.getCustomerPhone()).append("\n");
        }
        bodyBuilder.append("Thoi gian nhan: ").append(claim.getCreatedAt() != null ? claim.getCreatedAt().toString().substring(0, 19) : "N/A").append("\n");
        bodyBuilder.append("Noi dung khiếu nai cua Anh/Chi:\n");
        bodyBuilder.append("> ").append(claim.getClaimContent() != null ? claim.getClaimContent() : "N/A").append("\n\n");
        bodyBuilder.append("═══════════════════════════════════════\n");
        bodyBuilder.append("PHAN HOI TU MSB\n");
        bodyBuilder.append("═══════════════════════════════════════\n");
        bodyBuilder.append(response).append("\n\n");
        bodyBuilder.append("═══════════════════════════════════════\n");
        bodyBuilder.append("THONG TIN LIEN HE\n");
        bodyBuilder.append("═══════════════════════════════════════\n");
        bodyBuilder.append("Hotline: 1900 1088 (24/7)\n");
        bodyBuilder.append("Email: support@msb.com\n");
        bodyBuilder.append("Ma tham chieu: #").append(claimRef).append(" (Anh/Chi vui long ghi ma nay khi lien he)\n\n");
        bodyBuilder.append("Tran trong,\n");
        bodyBuilder.append("MSB Customer Care");

        String fullBody = bodyBuilder.toString();

        emailService.send(toEmail, subject, fullBody, isMockEmail);

        EmailLog emailLog = new EmailLog();
        emailLog.setClaimId(claimId);
        emailLog.setToEmail(toEmail);
        emailLog.setSubject(subject);
        emailLog.setBody(fullBody);
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

    @Transactional
    public void deleteClaim(UUID claimId) {
        claimRepository.deleteById(claimId);
        log.info("Claim deleted: id={}", claimId);
    }
}
