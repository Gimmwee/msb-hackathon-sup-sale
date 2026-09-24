package com.msb.supsale.controller;

import com.msb.supsale.model.Claim;
import com.msb.supsale.model.Message;
import com.msb.supsale.repository.UserRepository;
import com.msb.supsale.service.ClaimService;
import com.msb.supsale.service.ConversationService;
import com.msb.supsale.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cc/claims")
public class ContactCenterController {

    private final ClaimService claimService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final CustomerService customerService;

    public ContactCenterController(ClaimService claimService, ConversationService conversationService,
                                   UserRepository userRepository, CustomerService customerService) {
        this.claimService = claimService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.customerService = customerService;
    }

    @GetMapping
    public List<Claim> getClaims(@RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isBlank()) {
            return claimService.getClaimsByStatus(status);
        }
        return claimService.getAllClaims();
    }

    @GetMapping("/{id}")
    public Claim getClaim(@PathVariable UUID id) {
        return claimService.getClaim(id);
    }

    @GetMapping("/{id}/messages")
    public List<Message> getClaimMessages(@PathVariable UUID id) {
        Claim claim = claimService.getClaim(id);
        return conversationService.getHistory(claim.getSessionId());
    }

    @GetMapping("/customer")
    public ResponseEntity<Map<String, Object>> getCustomerInfo(@RequestParam String phone) {
        var customer = customerService.findByPhone(phone);
        if (customer.isEmpty()) return ResponseEntity.notFound().build();
        var c = customer.get();
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("name", c.getName());
        info.put("phone", c.getPhone());
        info.put("email", c.getEmail() != null ? c.getEmail() : "");
        info.put("idNumber", c.getIdNumber() != null ? c.getIdNumber() : "");
        info.put("address", c.getAddress() != null ? c.getAddress() : "");
        return ResponseEntity.ok(info);
    }

    @PostMapping("/{id}/approve")
    public Claim approve(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UUID resolvedBy = getCurrentUserId();
        String editedResponse = body.get("response");
        String email = body.get("email");
        if (email != null && !email.isBlank()) {
            Claim claim = claimService.getClaim(id);
            if (claim.getCustomerPhone() != null && !claim.getCustomerPhone().isBlank()) {
                customerService.updateEmail(claim.getCustomerPhone(), email);
            }
        }
        return claimService.approveClaim(id, resolvedBy, editedResponse);
    }

    @PostMapping("/{id}/abort")
    public Claim abort(@PathVariable UUID id) {
        UUID resolvedBy = getCurrentUserId();
        return claimService.abortClaim(id, resolvedBy);
    }

    @DeleteMapping("/{id}")
    public void deleteClaim(@PathVariable UUID id) {
        claimService.deleteClaim(id);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username).orElseThrow().getId();
    }
}
