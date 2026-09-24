package com.msb.supsale.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailService(@Value("${spring.mail.host:}") String smtpHost,
                            JavaMailSender mailSender,
                            @Value("${mail.from:noreply@sup-sale.msb.demo}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String to, String subject, String body, boolean isMock) {
        if (isMock) {
            log.info("[MOCK EMAIL] To: {} | Subject: {} (skipped real send)", to, subject);
            return;
        }
        try {
            String htmlBody = buildHtmlEmail(subject, body);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }

    private String buildHtmlEmail(String subject, String body) {
        String[] sections = body.split("(?=═{10,})");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,Helvetica,sans-serif'>");
        html.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:24px'>");
        html.append("<tr><td align='center'><table width='620' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08)'>");

        html.append("<tr><td style='background:linear-gradient(135deg,#E31837 0%,#b0142b 100%);padding:28px 32px;text-align:center'>");
        html.append("<h1 style='color:#ffffff;margin:0;font-size:24px;font-weight:800;letter-spacing:1px'>MSB BANK</h1>");
        html.append("<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px'>Hang dau dich vu tai chinh</p>");
        html.append("</td></tr>");

        html.append("<tr><td style='padding:28px 32px 8px'>");
        html.append("<h2 style='color:#0f172a;margin:0 0 20px;font-size:17px;font-weight:700;border-bottom:2px solid #E31837;padding-bottom:10px'>").append(subject).append("</h2>");
        html.append("</td></tr>");

        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("═")) {
                String header = trimmed.replaceAll("═+", "").trim();
                html.append("<tr><td style='padding:0 32px'>");
                html.append("<div style='background:#f8fafc;border-left:4px solid #E31837;padding:10px 14px;margin:12px 0;font-size:13px;font-weight:700;color:#E31837;text-transform:uppercase;letter-spacing:0.5px'>").append(header.split("\n")[0]).append("</div>");
                String content = trimmed.substring(trimmed.indexOf("\n") + 1).trim();
                html.append("<div style='padding:0 14px 8px;font-size:14px;color:#334155;line-height:1.8'>");
                for (String line : content.split("\n")) {
                    if (line.trim().startsWith(">")) {
                        html.append("<div style='background:#fef2f2;border-left:3px solid #fca5a5;padding:10px 14px;margin:8px 0;font-style:italic;color:#991b1b;border-radius:4px'>").append(line.trim().substring(1).trim()).append("</div>");
                    } else if (line.trim().startsWith("Ma khieu nai:") || line.trim().startsWith("Ma tham chieu:")) {
                        html.append("<div style='margin:4px 0'><strong style='color:#E31837'>").append(line.trim()).append("</strong></div>");
                    } else if (!line.trim().isEmpty()) {
                        html.append("<div style='margin:3px 0'>").append(line.trim()).append("</div>");
                    }
                }
                html.append("</div>");
                html.append("</td></tr>");
            } else {
                html.append("<tr><td style='padding:0 32px 12px;font-size:14px;color:#334155;line-height:1.8'>");
                for (String line : trimmed.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        html.append("<div style='margin:4px 0'>").append(line.trim()).append("</div>");
                    }
                }
                html.append("</td></tr>");
            }
        }

        html.append("<tr><td style='padding:24px 32px;background:#f8fafc;border-top:1px solid #e2e8f0'>");
        html.append("<p style='color:#64748b;font-size:12px;margin:0;line-height:1.8'>");
        html.append("<strong style='color:#E31837;font-size:13px'>MSB Customer Care</strong><br>");
        html.append("Hotline: <strong>1900 1088</strong> (24/7) | Email: support@msb.com<br>");
        html.append("© 2026 MSB Bank. All rights reserved.<br>");
        html.append("<span style='color:#94a3b8;font-size:11px'>Email nay duoc gui tu dong. Vui long khong reply.</span>");
        html.append("</p>");
        html.append("</td></tr>");

        html.append("</table></td></tr>");
        html.append("</table></body></html>");
        return html.toString();
    }
}
