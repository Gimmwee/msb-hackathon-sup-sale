package com.msb.supsale.controller;

import com.msb.supsale.agent.AgentOrchestrator;
import com.msb.supsale.dto.ChatRequest;
import com.msb.supsale.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private final AgentOrchestrator orchestrator;

    public ChatController(AgentOrchestrator orchestrator) { this.orchestrator = orchestrator; }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        AgentOrchestrator.AgentResponse result = orchestrator.chat(
                request.getSessionId(), request.getPlatform(), request.getMessage()
        );
        return new ChatResponse(request.getSessionId(), result.message(), result.intent(), result.leadCaptured());
    }
}
