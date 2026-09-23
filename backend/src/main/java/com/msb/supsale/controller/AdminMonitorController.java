package com.msb.supsale.controller;

import com.msb.supsale.config.GreenNodeConfig;
import com.msb.supsale.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMonitorController {

    private final JdbcTemplate jdbcTemplate;
    private final GreenNodeConfig config;
    private final LeadRepository leadRepo;
    private final CustomerRepository customerRepo;
    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final TransactionRepository txnRepo;
    private final CicRecordRepository cicRepo;
    private final ClaimRepository claimRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final SaleActivityRepository saleActRepo;

    public AdminMonitorController(JdbcTemplate jdbcTemplate, GreenNodeConfig config,
                                  LeadRepository leadRepo, CustomerRepository customerRepo,
                                  ConversationRepository convRepo, MessageRepository msgRepo,
                                  TransactionRepository txnRepo, CicRecordRepository cicRepo,
                                  ClaimRepository claimRepo, UserRepository userRepo,
                                  ProductRepository productRepo, SaleActivityRepository saleActRepo) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
        this.leadRepo = leadRepo;
        this.customerRepo = customerRepo;
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.txnRepo = txnRepo;
        this.cicRepo = cicRepo;
        this.claimRepo = claimRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.saleActRepo = saleActRepo;
    }

    @GetMapping("/monitor")
    public Map<String, Object> monitor() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());

        Map<String, Object> be = new LinkedHashMap<>();
        be.put("status", "UP");
        be.put("uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + "s");
        be.put("heapUsed", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        be.put("heapMax", Runtime.getRuntime().maxMemory());
        result.put("backend", be);

        Map<String, Object> db = new LinkedHashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            db.put("status", "UP");
            db.put("type", "PostgreSQL");
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("customers", customerRepo.count());
            counts.put("leads", leadRepo.count());
            counts.put("conversations", convRepo.count());
            counts.put("messages", msgRepo.count());
            counts.put("transactions", txnRepo.count());
            counts.put("cicRecords", cicRepo.count());
            counts.put("claims", claimRepo.count());
            counts.put("users", userRepo.count());
            counts.put("products", productRepo.count());
            counts.put("saleActivities", saleActRepo.count());
            db.put("tables", counts);
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        result.put("database", db);

        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("status", config.isMock() ? "MOCK" : "ACTIVE");
        agent.put("model", config.getModelCustomer());
        agent.put("mockMode", config.isMock());
        agent.put("apiConfigured", !config.getApiKey().isEmpty());
        result.put("agent", agent);

        Map<String, Object> fe = new LinkedHashMap<>();
        fe.put("status", "UP");
        fe.put("servedFrom", "embedded-static");
        result.put("frontend", fe);

        return result;
    }
}
