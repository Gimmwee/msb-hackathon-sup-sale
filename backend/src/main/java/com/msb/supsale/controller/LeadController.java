package com.msb.supsale.controller;

import com.msb.supsale.dto.LeadDto;
import com.msb.supsale.model.Customer;
import com.msb.supsale.service.CustomerService;
import com.msb.supsale.service.LeadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LeadController {
    private final LeadService leadService;
    private final CustomerService customerService;
    private final String dashboardApiKey;

    public LeadController(LeadService leadService, CustomerService customerService,
                          @Value("${dashboard.api-key}") String dashboardApiKey) {
        this.leadService = leadService;
        this.customerService = customerService;
        this.dashboardApiKey = dashboardApiKey;
    }

    @GetMapping("/leads")
    public List<LeadDto> getLeads(@RequestHeader(value = "X-API-Key", defaultValue = "") String apiKey) {
        validateApiKey(apiKey);
        return leadService.getAllLeads().stream().map(lead -> {
            LeadDto dto = new LeadDto(lead);
            customerService.findByPhone(lead.getPhone()).ifPresent(customer -> {
                dto.setIdNumber(customer.getIdNumber());
                dto.setDob(customer.getDob());
                dto.setGender(customer.getGender());
                dto.setAddress(customer.getAddress());
            });
            return dto;
        }).toList();
    }

    @GetMapping("/leads/metrics")
    public Map<String, Object> getMetrics(@RequestHeader(value = "X-API-Key", defaultValue = "") String apiKey) {
        validateApiKey(apiKey);
        return Map.of(
                "totalLeads", leadService.countTotal(),
                "newLeads", leadService.countByStatus("NEW"),
                "contactedLeads", leadService.countByStatus("CONTACTED")
        );
    }

    private void validateApiKey(String apiKey) {
        if (dashboardApiKey != null && !dashboardApiKey.isEmpty() && !dashboardApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key");
        }
    }
}
