package com.msb.supsale.controller;

import com.msb.supsale.dto.LeadDto;
import com.msb.supsale.model.Lead;
import com.msb.supsale.model.Message;
import com.msb.supsale.model.SaleActivity;
import com.msb.supsale.model.User;
import com.msb.supsale.repository.UserRepository;
import com.msb.supsale.service.ConversationService;
import com.msb.supsale.service.LeadService;
import com.msb.supsale.service.SaleActivityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sale")
public class SaleController {

    private final LeadService leadService;
    private final ConversationService conversationService;
    private final SaleActivityService saleActivityService;
    private final UserRepository userRepository;

    public SaleController(LeadService leadService, ConversationService conversationService,
                          SaleActivityService saleActivityService, UserRepository userRepository) {
        this.leadService = leadService;
        this.conversationService = conversationService;
        this.saleActivityService = saleActivityService;
        this.userRepository = userRepository;
    }

    @GetMapping("/leads")
    public List<LeadDto> getLeads() {
        return leadService.getAllLeads().stream().map(LeadDto::new).toList();
    }

    @GetMapping("/leads/{sessionId}/messages")
    public List<Message> getLeadMessages(@PathVariable String sessionId) {
        return conversationService.getHistory(sessionId);
    }

    @PostMapping("/leads/{leadId}/activity")
    public SaleActivity logActivity(@PathVariable UUID leadId, @RequestBody Map<String, String> body) {
        UUID saleUserId = getCurrentUserId();
        String action = body.getOrDefault("action", "NOTE");
        String note = body.getOrDefault("note", "");

        if ("CONTACTED".equals(action)) {
            leadService.updateStatus(leadId, "CONTACTED");
        } else if ("CONVERTED".equals(action)) {
            leadService.updateStatus(leadId, "CONVERTED");
        }

        return saleActivityService.logActivity(leadId, saleUserId, action, note);
    }

    @GetMapping("/leads/{leadId}/activities")
    public List<SaleActivity> getActivities(@PathVariable UUID leadId) {
        return saleActivityService.getByLead(leadId);
    }

    @GetMapping("/kpi")
    public Map<String, Object> getKpi() {
        UUID saleUserId = getCurrentUserId();
        List<SaleActivity> activities = saleActivityService.getBySaleUser(saleUserId);
        long contacted = activities.stream().filter(a -> "CONTACTED".equals(a.getAction())).count();
        long converted = activities.stream().filter(a -> "CONVERTED".equals(a.getAction())).count();
        double conversionRate = contacted > 0 ? (double) converted / contacted * 100 : 0;

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalActivities", activities.size());
        result.put("contacted", contacted);
        result.put("converted", converted);
        result.put("conversionRate", Math.round(conversionRate * 100) / 100.0);
        result.put("dailyActivity", saleActivityService.getDailyActivity(saleUserId, 14));
        return result;
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username).orElseThrow().getId();
    }
}
