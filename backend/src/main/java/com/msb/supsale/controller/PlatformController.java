package com.msb.supsale.controller;

import com.msb.supsale.agent.AgentOrchestrator;
import com.msb.supsale.dto.ChatRequest;
import com.msb.supsale.dto.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class PlatformController {

    private final AgentOrchestrator orchestrator;

    public PlatformController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy");
    }

    @PostMapping("/invocations")
    public ChatResponse invocations(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.getOrDefault("sessionId", "platform-" + System.currentTimeMillis());
        String platform = (String) payload.getOrDefault("platform", "WEB");
        String message = (String) payload.getOrDefault("message", "");
        AgentOrchestrator.AgentResponse result = orchestrator.chat(sessionId, platform, message);
        return new ChatResponse(sessionId, result.message(), result.intent(), result.leadCaptured());
    }
}
