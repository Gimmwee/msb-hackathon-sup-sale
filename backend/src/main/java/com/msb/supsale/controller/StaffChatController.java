package com.msb.supsale.controller;

import com.msb.supsale.agent.StaffAgentOrchestrator;
import com.msb.supsale.model.User;
import com.msb.supsale.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffChatController {

    private final StaffAgentOrchestrator staffAgent;
    private final UserRepository userRepository;

    public StaffChatController(StaffAgentOrchestrator staffAgent, UserRepository userRepository) {
        this.staffAgent = staffAgent;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "staff-" + System.currentTimeMillis());
        String message = request.getOrDefault("message", "");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        UUID staffUserId = user.getId();

        StaffAgentOrchestrator.StaffAgentResponse result = staffAgent.chat(sessionId, message, staffUserId);

        return Map.of(
                "message", result.message(),
                "lookupType", result.lookupType(),
                "sessionId", sessionId
        );
    }
}
