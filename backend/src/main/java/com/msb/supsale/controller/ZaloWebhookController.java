package com.msb.supsale.controller;

import com.msb.supsale.agent.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class ZaloWebhookController {
    private static final Logger log = LoggerFactory.getLogger(ZaloWebhookController.class);
    private final AgentOrchestrator orchestrator;

    public ZaloWebhookController(AgentOrchestrator orchestrator) { this.orchestrator = orchestrator; }

    @PostMapping("/zalo")
    public Map<String, Object> zaloWebhook(@RequestBody Map<String, Object> body) {
        String event = (String) body.getOrDefault("event_name", body.get("event"));
        if (!"usersendmsg".equals(event)) {
            return Map.of("status", "ignored");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> sender = (Map<String, Object>) body.getOrDefault("sender", Map.of());
        String userId = String.valueOf(sender.getOrDefault("id", body.get("userid")));
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) body.getOrDefault("message", Map.of());
        String text = (String) message.getOrDefault("text", "");

        if (userId.isEmpty() || text.isEmpty()) {
            return Map.of("status", "ignored");
        }

        String sessionId = "zalo:" + userId;
        AgentOrchestrator.AgentResponse result = orchestrator.chat(sessionId, "ZALO", text);
        log.info("Zalo webhook processed: user={}, leadCaptured={}", userId, result.leadCaptured());

        return Map.of("status", "ok", "reply", result.message(), "leadCaptured", result.leadCaptured());
    }
}
