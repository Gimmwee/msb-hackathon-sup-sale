package com.msb.supsale.service;

import com.msb.supsale.model.EmailLog;
import com.msb.supsale.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnMissingBean(EmailService.class)
public class MockEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    private final EmailLogRepository emailLogRepository;
    private final String fromEmail;

    public MockEmailService(EmailLogRepository emailLogRepository,
                            @Value("${mail.from:noreply@sup-sale.msb.demo}") String fromEmail) {
        this.emailLogRepository = emailLogRepository;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String to, String subject, String body, boolean isMock) {
        log.info("[MOCK EMAIL] To: {} | Subject: {} | Body preview: {}",
                to, subject, body != null ? body.substring(0, Math.min(100, body.length())) : "");
        log.info("[MOCK EMAIL] From: {} — no real email sent (mock mode)", fromEmail);
    }
}
